from flask import Flask, request, render_template, Response
import json
$MODULE_IMPORT$

app = Flask(__name__)

SERVICE_NAME = $SERVICE_NAME$
IMPLEMENTATIONS = $IMPLEMENTATIONS$
PARAMETERS = $PARAMETERS$


@app.route('/', methods=["GET", "POST", "OPTIONS"])
def service():
    if request.method == "GET":
        return render_template("service.html", service_active="active", service_name=SERVICE_NAME,
                               parameters=PARAMETERS, implementations=IMPLEMENTATIONS)
    elif request.method == "POST":
        params = request.get_json()
        impl_id = params.get("impl_id")
        try:
            result = $MAIN$
        except (ValueError, TypeError) as e:
            return Response(str(e), status=400)
        except Exception as e:
            return Response(str(e), status=500)
        return Response(json.dumps(result))

@app.route('/api')
def api():
    return render_template("api.html", service_name=SERVICE_NAME, parameters=PARAMETERS, implementations=IMPLEMENTATIONS)


if __name__ == '__main__':
    app.run()
