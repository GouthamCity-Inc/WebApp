package edu.northeastern.gatewayapplication.utils

import com.amazonaws.services.sns.AmazonSNS
import com.amazonaws.services.sns.AmazonSNSClient
import com.amazonaws.services.sns.AmazonSNSClientBuilder
import com.amazonaws.services.sns.model.PublishRequest
import com.amazonaws.services.sns.model.Topic
import java.util.Optional

class AmazonSNSUtils {

    private val client: AmazonSNS = AmazonSNSClientBuilder.defaultClient()

    fun filterTopicByName(arn: String): Optional<Topic>{
        val topics = client.listTopics().topics
        return topics.stream().filter {topic ->
            topic.topicArn.endsWith(arn)
        }.findFirst()
    }


    fun getPublishRequest(arn: String, message: String): PublishRequest{
        return PublishRequest(arn, message)
    }

    fun publishMessage(request: PublishRequest){
        client.publish(request)
    }
}