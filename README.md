# BusRadar
BusRadar Gdańsk: punktualność komunikacji miejskiej w czasie rzeczywistym na otwartych danych ZTM w Gdańsku


# How to start the environment
cd infra && docker compose up -d
and after its done, check the status using docker ps
(However make sure that in infra/postgres/init/01-create-databases.sh uses LF convention instead of CRLF)