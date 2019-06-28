ui:
  features:
    accountability: true
    completion: true
    compliance: true
    freezeAndDefrost: true
    managementFeatureEnrichment: true
    nfv: true
    patternRefinement: true
    problemDetection: true
    splitting: true
    testRefinement: true
  endpoints:
    container: http\://{{ .Env.CONTAINER_HOSTNAME }}\:{{ .Env.CONTAINER_PORT }}
    workflowmodeler: http\://{{ .Env.WORKFLOWMODELER_HOSTNAME }}\:{{ .Env.WORKFLOWMODELER_PORT }}/winery-workflowmodeler
    topologymodeler: http\://{{ .Env.TOPOLOGYMODELER_HOSTNAME }}\:{{ .Env.TOPOLOGYMODELER_PORT }}/winery-topologymodeler
    repositoryApiUrl: http://localhost:8080/winery
    repositoryUiUrl: http://localhost:8080/#
repository:
  provider: file
  repositoryRoot: {{ .Env.WINERY_REPOSITORY_PATH }}
  git:
    clientSecret: secret
    password: default
    clientID: id
    autocommit: false
    username: default
