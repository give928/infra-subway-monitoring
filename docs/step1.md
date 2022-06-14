# 서비스 진단하기

## 1단계 - 로깅과 모니터링

### 요구사항
- [x] 애플리케이션 진단하기 실습을 진행해보고 문제가 되는 코드를 수정
- [x] 로그 설정하기
- [x] Cloudwatch로 모니터링

### 요구사항 설명
- 저장소를 활용하여 아래 요구사항을 해결합니다.
- README 에 있는 질문에 답을 추가한 후 PR을 보내고 리뷰요청을 합니다.

#### 로그 설정하기
- [x] Application Log 파일로 저장하기
  - 회원가입, 로그인 등의 이벤트에 로깅을 설정
  - 경로찾기 등의 이벤트 로그를 JSON으로 수집
  ```shell
  $ sudo mkdir /var/log/app
  $ cd ~/nextstep/nextstep/infra-subway-monitoring
  $ ./gradlew jibDockerBuild
  $ sudo docker run -d -p 8080:8080 -v /var/log/app:/var/log/app --name infra-subway-monitoring infra-subway-monitoring
  ```
- [x] Nginx Access Log 설정하기
  ```shell
  $ sudo mkdir /var/log/nginx
  $ sudo docker run -d -p 80:80 -p 443:443 --name proxy -v /var/log/nginx:/var/log/nginx reverse-proxy:0.0.2
  ```

#### Cloudwatch로 모니터링
- [x] Cloudwatch로 로그 수집하기
- [x] Cloudwatch로 메트릭 수집하기
- [x] USE 방법론을 활용하기 용이하도록 대시보드 구성

##### EC2에 IAM role 설정
- CloudWatchAgentServerPolicy, AmazonSSMManagedInstanceCore 권한 추가
- 작업 > 보안 > IAM 역할 수정 > IAM 역할 설정

##### cloudwatch logs agent 설치
```shell
# python 설치
$ sudo apt install python2.7 # cloudwatch logs agent only supports python version 2.6 - 3.5
$ ls /usr/bin/python*
$ sudo update-alternatives --install /usr/bin/python python /usr/bin/python2.7 1
```

```shell
# cloudwatch logs agent를 설치합니다.
$ curl https://s3.amazonaws.com/aws-cloudwatch/downloads/latest/awslogs-agent-setup.py -O
$ sudo python ./awslogs-agent-setup.py --region ap-northeast-2
```

##### 로그 수집
```shell
$ sudo vi /var/awslogs/etc/awslogs.conf

[/var/log/syslog]
datetime_format = %Y-%m-%d %H:%M:%S
file = /var/log/syslog
buffer_duration = 5000
log_stream_name = {instance_id}
initial_position = start_of_file
log_group_name = give928

[/var/log/nginx/access.log]
datetime_format = %Y-%m-%d %H:%M:%S %z
file = /var/log/nginx/access.log
buffer_duration = 5000
log_stream_name = access.log
initial_position = end_of_file
log_group_name = give928

[/var/log/nginx/error.log]
datetime_format = %Y-%m-%d %H:%M:%S
file = /var/log/nginx/error.log
buffer_duration = 5000
log_stream_name = error.log
initial_position = end_of_file
log_group_name = give928
```
```shell
$ sudo service awslogs restart
```

##### Metric 수집
```shell
$ wget https://s3.amazonaws.com/amazoncloudwatch-agent/ubuntu/amd64/latest/amazon-cloudwatch-agent.deb
$ sudo dpkg -i -E ./amazon-cloudwatch-agent.deb
```

```shell
$ sudo vi /opt/aws/amazon-cloudwatch-agent/bin/config.json

{
    "agent": {
        "metrics_collection_interval": 60,
        "run_as_user": "root"
    },
    "metrics": {
        "metrics_collected": {
            "disk": {
                "measurement": [
                    "used_percent",
                    "used",
                    "total"
                ],
                "metrics_collection_interval": 60,
                "resources": [
                    "*"
                ]
            },
            "mem": {
                "measurement": [
                    "mem_used_percent",
                    "mem_total",
                    "mem_used"
                ],
                "metrics_collection_interval": 60
            }
        }
    }
}
```

```shell
$ sudo /opt/aws/amazon-cloudwatch-agent/bin/amazon-cloudwatch-agent-ctl -a fetch-config -m ec2 -s -c file:/opt/aws/amazon-cloudwatch-agent/bin/config.json
$ sudo /opt/aws/amazon-cloudwatch-agent/bin/amazon-cloudwatch-agent-ctl -m ec2 -a status
{
  "status": "running",
  "starttime": "2022-06-10T12:03:10+00:00",
  "configstatus": "configured",
  "cwoc_status": "stopped",
  "cwoc_starttime": "",
  "cwoc_configstatus": "not configured",
  "version": "1.247352.0b251908"
}
```

##### CloudWatch > Dashboards
위젯 추가 > 유형으로 행 선택 > 원본데이터로 지표 선택 > CPU Utilization, Network In / Out, mem_used_percent, disk_used_percent 등을 추가

##### Spring Actuator Metric 수집
```
dependencies {
	implementation 'org.springframework.boot:spring-boot-starter-actuator'
	implementation 'org.springframework.cloud:spring-cloud-starter-aws:2.2.1.RELEASE'
	implementation 'io.micrometer:micrometer-registry-cloudwatch'
}    
```
```properties
# 로컬에서 실행시 AWS stack autoconfiguration 수행과정에서 발생하는 에러 방지
cloud.aws.stack.auto=false
cloud.aws.region.static=ap-northeast-2
management.metrics.export.cloudwatch.namespace=give928-metric # 해당 namespace로 Cloudwatch 메트릭을 조회 가능
management.metrics.export.cloudwatch.batch-size=20
management.endpoints.web.exposure.include=*
```
