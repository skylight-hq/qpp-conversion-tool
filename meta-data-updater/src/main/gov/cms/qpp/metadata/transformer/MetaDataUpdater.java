package main.gov.cms.qpp.metadata.transformer;

import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBMapper;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBQueryExpression;
import com.amazonaws.services.dynamodbv2.datamodeling.PaginatedQueryList;
import com.amazonaws.services.dynamodbv2.model.AttributeValue;
import main.gov.cms.qpp.metadata.config.DynamoDbConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import gov.cms.qpp.conversion.api.model.Constants;
import gov.cms.qpp.conversion.api.model.Metadata;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class MetaDataUpdater {
	private static final Logger logs = LoggerFactory.getLogger(MetaDataUpdater.class);

	/**
	 * Main method for handling the export and import of tables.
	 *
	 * @param args
	 */
	public static void main(String[] args) {
		if (args.length == 3) {
		DynamoDbConfig config = new DynamoDbConfig();
		DynamoDBMapper mapper = config.dynamoDbMapper(config.dynamoDbClient(), args);

		List<Metadata> metadataList = getAllFiles(mapper);
		updateData(metadataList, mapper);
		} else {
			logs.info("Please pass the KMS key ARN and Dynamodb Table name respectively as arguments");
		}
	}

	private static List<Metadata> getAllFiles(DynamoDBMapper mapper)  {
		return IntStream.range(0, Constants.CPC_DYNAMO_PARTITIONS).mapToObj(partition -> {
			Map<String, AttributeValue> valueMap = new HashMap<>();
			valueMap.put(":cpcValue", new AttributeValue().withS(Constants.CPC_DYNAMO_PARTITION_START + partition));
			valueMap.put(":cpcProcessedValue", new AttributeValue().withS("true#2020"));

			DynamoDBQueryExpression<Metadata> metadataQuery = new DynamoDBQueryExpression<Metadata>()
				.withIndexName("Cpc-CpcProcessed_CreateDate-index")
				.withKeyConditionExpression(Constants.DYNAMO_CPC_ATTRIBUTE + " = :cpcValue and begins_with("
					+ Constants.DYNAMO_CPC_PROCESSED_CREATE_DATE_ATTRIBUTE + ", :cpcProcessedValue)")
				.withExpressionAttributeValues(valueMap)
				.withConsistentRead(false);
			PaginatedQueryList<Metadata> pql = mapper.query(Metadata.class, metadataQuery);
			pql.loadAllResults();

			return pql.stream();
		}).flatMap(Function.identity()).collect(Collectors.toList());
	}

	private static void updateData(final List<Metadata> metadataList, DynamoDBMapper mapper) {
		logs.info("Performing update on items...");
		AtomicInteger count = new AtomicInteger();

		metadataList.forEach(metadata -> {
			int itemPosition = count.incrementAndGet();
			if ( itemPosition % 500 == 0) {
				logs.info("Updated {} items. Sleeping for one second...",
					itemPosition);
				pauseExecution();
			}
			metadata.setCpcProcessed(false);
			mapper.save(metadata);
		});
		logs.info("Finished updating items...");
	}

	private static void pauseExecution() {
		try {
			Thread.sleep(1000);
		}
		catch (InterruptedException e) {
			logs.info("Sleep has been interrupted!");
		}
	}
}
