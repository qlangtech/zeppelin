mvn install -DskipRat -DskipTests=true -pl jdbc-tis,elasticsearch-tis,zeppelin-server,zeppelin-client -am \
-DaltDeploymentRepository=base::default::http://localhost:8080/snapshot
