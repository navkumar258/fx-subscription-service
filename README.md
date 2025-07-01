# fx-subscription-service
FX Subscription Service

Run prometheus container wth cusotm config file:

`docker run -d --name prometheus -it -p 9090:9090 -v ./prometheus.yml:/etc/prometheus/prometheus.yml prom/prometheus`
