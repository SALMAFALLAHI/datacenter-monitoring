/*package com.example.demo.services;

import com.example.demo.entity.Equipement;
import com.example.demo.entity.Metrique;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.cloudwatch.CloudWatchClient;
import software.amazon.awssdk.services.cloudwatch.model.*;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class AwsCloudWatchService {

    public List<Metrique> fetchMetrics(Equipement eq, String region, int minutesBack) {
        if (eq.getCloudInstanceId() == null) return List.of();

        CloudWatchClient client = CloudWatchClient.builder()
                .region(Region.of(region))
                .build();

        Instant end = Instant.now();
        Instant start = end.minusSeconds(minutesBack * 60L);

        Double cpu      = getMetric(client, "AWS/EC2", "CPUUtilization", eq.getCloudInstanceId(), start, end);
        Double netIn    = getMetric(client, "AWS/EC2", "NetworkIn",      eq.getCloudInstanceId(), start, end);
        Double netOut   = getMetric(client, "AWS/EC2", "NetworkOut",     eq.getCloudInstanceId(), start, end);
        Double diskRead = getMetric(client, "AWS/EC2", "EBSReadBytes",   eq.getCloudInstanceId(), start, end);
        Double diskWrite= getMetric(client, "AWS/EC2", "EBSWriteBytes",  eq.getCloudInstanceId(), start, end);

        Metrique m = new Metrique();
        m.setEquipement(eq);
        m.setDateCollecte(LocalDateTime.now());
        m.setCpu(cpu != null ? cpu.floatValue() : 0.0f);
        m.setRamPct(0.0);
        m.setUsedGb(0.0);
        m.setAvailableGb(0.0);
        m.setSwapPct(0.0);
        m.setReseau((float) ((netIn != null && netOut != null) ? (netIn + netOut) / 1024.0 : 0.0));
        m.setDisque((float) ((diskRead != null && diskWrite != null) ? (diskRead + diskWrite) / (1024.0 * 1024.0 * 1024.0) : 0.0));
        m.setTemperature(null);

        client.close();
        return List.of(m);
    }

    private Double getMetric(CloudWatchClient client, String namespace, String metricName,
                             String instanceId, Instant start, Instant end) {
        try {
            GetMetricStatisticsRequest request = GetMetricStatisticsRequest.builder()
                    .namespace(namespace)
                    .metricName(metricName)
                    .dimensions(Dimension.builder().name("InstanceId").value(instanceId).build())
                    .startTime(start)
                    .endTime(end)
                    .period(60)
                    .statistics(Statistic.AVERAGE)
                    .build();

            GetMetricStatisticsResponse response = client.getMetricStatistics(request);
            return response.datapoints().stream()
                    .findFirst()
                    .map(Datapoint::average)
                    .orElse(null);
        } catch (Exception e) {
            return null;
        }
    }
}*/
//simulation de données réalistes

package com.example.demo.services;

import com.example.demo.entity.Equipement;
import com.example.demo.entity.Metrique;
import com.example.demo.entity.TypeEquipement;
import com.example.demo.entity.TypeInfrastructure;


import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

@Service
@ConditionalOnProperty(name = "aws.mock.enabled", havingValue = "true", matchIfMissing = true)
public class AwsCloudWatchService {

    public List<Metrique> fetchMetrics(Equipement eq, String region, int minutesBack) {
        if (eq.getCloudInstanceId() == null) return List.of();

        // Simulation de données réalistes
        float cpu = ThreadLocalRandom.current().nextFloat() * 80f;        // 0-80%
        float net = ThreadLocalRandom.current().nextFloat() * 5000f;      // KB
        float disk = ThreadLocalRandom.current().nextFloat() * 200f;      // GB

        Metrique m = new Metrique();
        m.setEquipement(eq);
        m.setDateCollecte(LocalDateTime.now());
        m.setCpu(cpu);
        m.setRamPct(0.0);           // CloudWatch de base ne donne pas la RAM
        m.setUsedGb(0.0);
        m.setAvailableGb(0.0);
        m.setSwapPct(0.0);
        m.setReseau(net);
        m.setDisque(disk);
        m.setTemperature(null);       
        m.setSource(TypeInfrastructure.CLOUD);  // NOUVEAU

        return List.of(m);
    }
}