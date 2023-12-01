# maven-enforcer-plugin for skip : -Denforcer.skip=true
#mvn deploy -Ptis-repo -DskipRat -Denforcer.skip=true -DskipTests=true \
#-pl jdbc-tis\
#,elasticsearch-tis\
#,zeppelin-server\
#,zeppelin-client\
#,zeppelin-package\
# -am -fn
mvn clean deploy -Pbuild-distr -DskipRat -DskipTests=true -Denforcer.skip=true -Dappname=all \
-pl jdbc-tis\
,elasticsearch-tis\
,zeppelin-server\
,zeppelin-client\
,zeppelin-package\
,tis-zeppelin\
,zeppelin-distribution\
,zeppelin-interpreter-shaded \
-am \
-Ptis-repo
