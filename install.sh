mvn install -Pbuild-distr -DskipRat -DskipTests=true -Dappname=all -pl jdbc-tis,elasticsearch-tis,zeppelin-server,zeppelin-client,zeppelin-package,tis-zeppelin,zeppelin-distribution,zeppelin-interpreter-shaded -am \
-DaltDeploymentRepository=base::default::http://localhost:8080/snapshot
