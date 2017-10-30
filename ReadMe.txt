1. Before compiling/running this project, please visit and obtain the spark kafka010 connector jar:

https://github.com/hortonworks-spark/skc

2. Include the jar into your local maven repo as follow:

mvn install:install-file -Dfile=<location of connection jar> -DgroupId=com.hortonworks -DartifactId=spark-kafka-0-10-connector_2.10 -Dversion=1.0.0 -Dpackaging=jar

For example:

mvn install:install-file -Dfile=/Users/ctam/Desktop/spark-streaming-kafka-0-10-connector-master/kafka-0-10-assembly/target/spark-kafka-0-10-connector-assembly_2.10-1.0.0.jar -DgroupId=com.hortonworks -DartifactId=spark-kafka-0-10-connector_2.10 -Dversion=1.0.0 -Dpackaging=jar

3. Compile this jar:

mvn clean pacakge

4. To connect to a secured Kafka Cluster, please go through the instruction first :

https://docs.hortonworks.com/HDPDocuments/HDP2/HDP-2.6.2/bk_spark-component-guide/content/using-spark-streaming.html#spark-streaming-jar

To conclude, in order to talk to a secured Kafka cluster you need:

-) In your job, pass in "security.protocol" configuration as either "SASL_PLAINTEXT"
-) A valid jaas file
-) The keytab file that matches the location name in the jaas file

An sample jaas file:

KafkaClient {
   com.sun.security.auth.module.Krb5LoginModule required
   useKeyTab=true
   keyTab="./spark.headless.keytab"
   storeKey=true
   useTicketCache=false
   serviceName="kafka"
   principal="spark-sparkb@SPARKBSECURE.COM";
};



5. To build this project use

    mvn package

6. Spark submit should conclude the following:

-) --files <your jass file>, <your keytab>
-) --driver-java-options "-Djava.security.auth.login.config=./<jass file>" \       ## Please note that this is using the current directory ./
-) --conf "spark.executor.extraJavaOptions=-Djava.security.auth.login.config=./key.conf"  ##current directory same as above




To create kafka topic:

Refer to the HCC article:

https://community.hortonworks.com/articles/79923/step-by-step-recipe-for-securing-kafka-with-kerber.html


1) cd /usr/hdp/current/kafka-broker/bin

##### create the topic
2) ./kafka-topics.sh --zookeeper sparkb1.sec.support.com:2181,sparkb2.sec.support.com:2181,sparkb4.sec.support.com:2181 --create --topic kafka010 --partitions 1 --replication-factor 2

##### provide producer permission
3) ./kafka-acls.sh --authorizer-properties sparkb1.sec.support.com:2181,sparkb2.sec.support.com:2181,sparkb4.sec.support.com:2181 --add --allow-principal User:spark-sparkb --producer --topic kafka010

##### provide consumer permission
4) ./kafka-acls.sh --authorizer-properties zookeeper.connect=sparkb1.sec.support.com:2181,sparkb2.sec.support.com:2181,sparkb4.sec.support.com:2181 --add --allow-principal User:spark-sparkb --consumer --topic kafka010

##### confirm the above ACL is there
5) ./kafka-acls.sh  --list --authorizer-properties zookeeper.connect=sparkb1.sec.support.com:2181,sparkb2.sec.support.com:2181,sparkb4.sec.support.com:2181

#####confirm you can produce as spark user
5) ./kafka-console-producer.sh --broker-list sparkb2.sec.support.com:6667,sparkb4.sec.support.com:6667,sparkb5.sec.support.com:6667 --topic kafka010 --security-protocol PLAINTEXTSASL

#####confirm you can consumer as spark user. Note protocol has to be PLAINTEXTSASL instead of
6) ./kafka-console-consumer.sh --zookeeper sparkb1.sec.support.com:2181,sparkb2.sec.support.com:2181,sparkb4.sec.support.com:2181 --topic kafka010 --security-protocol PLAINTEXTSASL

To submit the spark job:

spark-submit --master yarn-cluster --jars <location of your spark kafk010 connector jar>  --files <location of keytab>,<location of your jass file> --driver-java-options "-Djava.security.auth.login.config=./<jass file name>" --conf "spark.executor.extraJavaOptions=-Djava.security.auth.login.config=./<jaas file name>" --class Kafka010ConnectorExample.Connector <application jar location> <broker list> <topic> <groupId> <security protocol>

For example:

spark-submit --master yarn-cluster --jars spark-kafka-0-10-connector-assembly_2.10-1.0.0.jar --files /etc/security/keytabs/spark.headless.keytab,./kafka.jaas --driver-java-options "-Djava.security.auth.login.config=./kafka.jaas" --conf "spark.executor.extraJavaOptions=-Djava.security.auth.login.config=./kafka.jaas" --class Kafka010ConnectorExample.Connector Example-1.0.jar sparkb2.sec.support.com:6667,sparkb4.sec.support.com:6667,sparkb5.sec.support.com:6667 kafka010 group1 SASL_PLAINTEXT

As this by default read from the latest offset, you can use the console producer to provide some input. Or, you can change the offset to from the begining:

https://spark.apache.org/docs/1.6.1/streaming-kafka-integration.html


##########
You may notice in the above example, the jaas file and the keyfile location are set to current directory in the jaas and java opt.
This is because in yarn mode, files are distributed to yarn container local dir. For example /data01/yarn/localdir.
In this case, if you put a absolute path "/home/me/xxx.jaas file", you would either get a "no such file", as this file does not exist on
the ndoemanager that runs the container, or you would have to make sure all nodemanager has /home/me/xxx.jaas file. Same goes to other
files like ssl trustore


