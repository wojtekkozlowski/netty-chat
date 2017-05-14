#!/bin/bash
mvn clean compile assembly:single -DmainClass=server.Server -DfinalName=server
mv target/server-jar-with-dependencies.jar .
mvn clean compile assembly:single -DmainClass=client.Client -DfinalName=client
mv target/client-jar-with-dependencies.jar .
