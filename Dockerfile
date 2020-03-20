FROM maven:3-jdk-8 as builder
COPY . /tmp/winery
WORKDIR /tmp/winery
RUN mvn package -DskipTests

FROM tomcat:9-jdk8
LABEL maintainer = "Oliver Kopp <kopp.dev@gmail.com>, Michael Wurster <miwurster@gmail.com>, Lukas Harzenetter <lharzenetter@gmx.de>"

ARG DOCKERIZE_VERSION=v0.3.0
ARG USER_ID=999

ENV WINERY_USER_HOME /opt/winery
ENV WINERY_REPOSITORY_URL ""
ENV WINERY_HEAP_MAX 2048m
ENV WINERY_JMX_ENABLED ""
ENV CONTAINER_HOSTNAME localhost
ENV CONTAINER_PORT 1337
ENV WORKFLOWMODELER_HOSTNAME localhost
ENV WORKFLOWMODELER_PORT 8080
ENV TOPOLOGYMODELER_HOSTNAME localhost
ENV TOPOLOGYMODELER_PORT 8080
ENV WINERY_REPOSITORY_PROVIDER "file"
ENV WINERY_REPOSITORY_PATH "/var/repository"
ENV WINERY_HOSTNAME localhost
ENV WINERY_PORT 8080
ENV EDMM_TRANSFORMATION_HOSTNAME localhost
ENV EDMM_TRANSFORMATION_PORT 5000
ENV WINERY_FEATURE_ACCOUNTABILITY false
ENV WINERY_FEATURE_TEST_COMPLETION false
ENV WINERY_FEATURE_TEST_COMPLIANCE false
ENV WINERY_FEATURE_FREEZE_DEFROST false
ENV WINERY_FEATURE_MANAGEMENT_ENRICHMENT false
ENV WINERY_FEATURE_NFV false
ENV WINERY_FEATURE_PATTERN_REFINEMENT false
ENV WINERY_FEATURE_PROBLEM_DETECTION false
ENV WINERY_FEATURE_RADON false
ENV WINERY_FEATURE_SPLITTING false
ENV WINERY_FEATURE_TEST_REFINEMENT false
ENV WINERY_FEATURE_EDMM_MODELING false

RUN rm /dev/random && ln -s /dev/urandom /dev/random \
    && curl -s https://packagecloud.io/install/repositories/github/git-lfs/script.deb.sh | bash \
    && apt-get update -qq && apt-get install -qqy \
        git \
        git-lfs \
    && apt-get clean \
    && rm -rf /var/lib/apt/lists/* \
    && rm -rf ${CATALINA_HOME}/webapps/* \
    && sed -ie "s/securerandom.source=file:\/dev\/random/securerandom.source=file:\/dev\/.\/urandom/g" /usr/local/openjdk-8/jre/lib/security/java.security \
    && wget https://github.com/jwilder/dockerize/releases/download/$DOCKERIZE_VERSION/dockerize-linux-amd64-$DOCKERIZE_VERSION.tar.gz \
    && tar -C /usr/local/bin -xzvf dockerize-linux-amd64-$DOCKERIZE_VERSION.tar.gz \
    && rm dockerize-linux-amd64-$DOCKERIZE_VERSION.tar.gz

COPY --from=builder /tmp/winery/org.eclipse.winery.repository.rest/target/winery.war ${CATALINA_HOME}/webapps/winery.war
COPY --from=builder /tmp/winery/org.eclipse.winery.frontends/target/tosca-management.war ${CATALINA_HOME}/webapps/ROOT.war
COPY --from=builder /tmp/winery/org.eclipse.winery.frontends/target/topologymodeler.war ${CATALINA_HOME}/webapps/winery-topologymodeler.war
COPY --from=builder /tmp/winery/org.eclipse.winery.frontends/target/workflowmodeler.war ${CATALINA_HOME}/webapps/winery-workflowmodeler.war

# create Winery user and home dir
RUN mkdir ${WINERY_USER_HOME}
RUN groupadd -g ${USER_ID} winery
RUN useradd -s /bin/nologin -u ${USER_ID} -g winery -d ${WINERY_USER_HOME} --system winery
RUN chown winery: ${WINERY_USER_HOME}

# create repository dir and change ownership
RUN mkdir /var/repository
RUN chmod a+rwx /var/repository
RUN chown winery: /var/repository

# workaround because catalina has to be able to write files in the catalina_home dir
RUN chmod -R a+w ${CATALINA_HOME}

# install git lfs and set git config
RUN git config --global core.fscache true
RUN git lfs install

# copy config
COPY --chown=winery:winery docker/winery.yml.tpl ${WINERY_USER_HOME}/winery.yml.tpl

# configure entrypoint
COPY --chown=winery:winery docker/docker-entrypoint.sh /docker-entrypoint.sh
RUN chmod +x /docker-entrypoint.sh

USER winery
WORKDIR ${WINERY_USER_HOME}

EXPOSE 8080

CMD ["/docker-entrypoint.sh"]
