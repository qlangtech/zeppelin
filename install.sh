mvn deploy -DskipRat -DskipTests=true -pl jdbc-tis,elasticsearch-tis,zeppelin-server,zeppelin-client,zeppelin-package -am \
-DaltDeploymentRepository=base::default::http://localhost:8080/snapshot
