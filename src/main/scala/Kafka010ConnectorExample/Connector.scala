package Kafka010ConnectorExample
import org.apache.spark.SparkContext
import org.apache.spark.streaming._
import scala.collection.mutable
import org.apache.kafka.clients.consumer.ConsumerConfig
import org.apache.kafka.common.serialization.StringDeserializer
import org.apache.spark.SparkConf
import org.apache.spark.streaming.{StreamingContext}
import org.apache.spark.streaming.kafka010.{ConsumerStrategies, KafkaUtils, LocationStrategies}
/**
  * Created by ctam on 10/26/17.
  * Copy from spark-kafka-connector-010
  */
object Connector {

  def main(args: Array[String])= {

    if (args.length < 3){
      println("please enter host:port, topic, interval, groupid")
      exit(1)
    }else{
      println(s"Talking to Kafka host ${args(0)}, to topic ${args(1)} every ${args(2)} second with groupid ${args(3)}")
    }

    val conf = new SparkConf().setAppName("KafkaToHive")
    val sc = new SparkContext(conf)
    val ssc = new StreamingContext(sc, Seconds(args(2).toInt))

    val kafkaParams = new mutable.HashMap[String, Object]()
    kafkaParams.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, args(0))
    kafkaParams.put(ConsumerConfig.GROUP_ID_CONFIG, args(3))
    kafkaParams.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, classOf[StringDeserializer])
    kafkaParams.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, classOf[StringDeserializer])


    val topicSet = args(1).split(",").map(_.trim).filter(!_.isEmpty).toSet

    val consumerStrategy = ConsumerStrategies.Subscribe[String, String](topicSet, kafkaParams)
    val stream = KafkaUtils.createDirectStream(
      ssc, LocationStrategies.PreferBrokers, consumerStrategy)

    stream.foreachRDD( x => x.foreach(println))
    val words = stream.flatMap { r => r.value().split(" ") }.map { r => (r, 1) }.reduceByKey(_ + _)
    words.print
    ssc.start()
    ssc.awaitTermination()
  }

}
