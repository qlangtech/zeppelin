# maven-enforcer-plugin for skip : -Denforcer.skip=true
mvn deploy -Ptis-repo -DskipRat -Denforcer.skip=true -DskipTests=true \
-pl jdbc-tis\
,elasticsearch-tis\
,zeppelin-server\
,zeppelin-client\
,zeppelin-package\
 -am -fn

