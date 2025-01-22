# 실습 과제
![과제](/img/lecture10-homework.png)

# 시나리오
1. namespace를 2개 생성 : edu-user, edu-goods
2. demo 서비스를 참고하여 edu-user, edu-goods 서비스 생성
   - API : edu-user.xxx.xxx.xxx.xxx.sslip.io/api/v1/user/{userNo}
           -> goods/api/v1/{goodsNo} 호출 결과 받아서
           -> return : userNo, userName(prod-userNo), goodsNo(userNo), goodsName(goods-prod-userNo)
   - API : edu-goods.xxx.xxx.xxx.xxx.sslip.io/api/v1/goods/{goodsNo}
           ->  goodsNo(goodsNo), goodsName(goods-prod-goodsNo) 
3. HPA 설정 : edu-user(min:2, max:4), edu-goods(min:2, max:2)
   - 조건 : cpu 평균 사용량 30% 이상일때 1개씩 pod 증가
4. porb 설정하여 pod 내 어플리케이션이 다운 되면 자동으로 pod start하게 설정
   - 체크 조건 : "/" 호출 하여 상태 체크
5. argocd 통해 배포

# 과제 Step
## 1. namespace 2개 생성: edu-user, edu-goods
```sh
kubectl create ns edu-user
kubectl create ns edu-goods
```
![alt text](/img/{CE150160-6286-4659-905E-67438F555620}.png)

## 2. demo 서비스를 참고하여 edu-user, edu-goods 서비스 생성

### 2.1. edu-goods 서비스용 인스턴스 개발
![alt text](/img/{17677101-E7D2-40E4-BC92-A806A8C01576}.png)
#### 2.1.1. GoodsController.java 생성
```java
package com.ktds.edugoods.controller;

import com.ktds.edugoods.dto.response.GetGoodsResDto;
import com.ktds.edugoods.service.GoodsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RequiredArgsConstructor
@RequestMapping("/api/v1/goods")
@RestController
public class GoodsController {
    private final GoodsService goodsService;

    @GetMapping("/{goodsNo}")
    public ResponseEntity<GetGoodsResDto> getGoods(@PathVariable int goodsNo) {
        try {
            GetGoodsResDto getGoodsResDto = goodsService.getGoods(goodsNo);
            return new ResponseEntity<>(getGoodsResDto, HttpStatus.OK);
        } catch (Exception e) {
            log.error(e.getMessage());
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
}
```
- `/api/v1/goods/{goodsNo}`를 호출하였을 때, 결과값으로 goodsNo와 goodsName을 반환하는 컨트롤러 생성.
- 이때, `goodsName`에 해당하는 값은 `goods-<app.run.type>-<goodsNo>`의 형식을 유지할 수 있도록 처리. (ex. goods-dev-1)
#### 2.1.2. GoodsService.java 생성
```java
package com.ktds.edugoods.service;

import com.ktds.edugoods.dto.response.GetGoodsResDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class GoodsService {

    @Value("${app.run.type}")
    private String env;

    public GetGoodsResDto getGoods(int goodsNo) {
        return GetGoodsResDto.builder()
                .goodsNo(goodsNo)
                .goodsName("goods-" + env + "-" + goodsNo)
                .build();
    }
}
```
- [GoodsController](#211-goodscontroller-생성)에서 언급했듯이, goodsName이 의도된 형식으로 출력되도록 로직을 설계하여 작성.
#### 2.1.3. application properties 설정
```yaml
# application.yml
spring:
  application:
    name: edu-goods
  profiles:
    active: loc

management: # 이후 liveness probe 구현을 위한 health checking 용 spring actuator 설정값
  endpoints:
    web:
      exposure:
        include: health
  endpoint:
    health:
      probes:
        enabled: true

# application-loc.yml
app:
  run:
    type: local

server:
  port: 8081 # 추후 개발될 edu-user의 port를 8080으로 해 둠으로써, port 겹침 문제를 해소하기 위해 edu-goods를 8081로 지정

logging:
  level:
    com.ktds: DEBUG

# application-prod.yml
app:
  run:
    type: prod

server:
  port: 8081

logging:
  level:
    com.ktds: DEBUG

# application-dev.yml 생략
```
- 각 환경 별 환경 변수 파일에 `app.run.type`을 환경에 해당하는 값을 기재하여, 추후 k8s 환경에서 배포 시 선언 된 env의 SPRING_PROFILES_ACTIVE 환경변수 값이 적용이 잘 되는지 확인할 수 있다.
#### 2.1.4. 로컬 환경에서의 api call 수행
![alt text](/img/{E7260C57-EAB2-4ACD-96E4-0B5E5D510101}.png)
- `http://localhost:8081/api/v1/goods/1` 수행 시 결과 확인.

### 2.2. edu-user 서비스용 인스턴스 개발
![alt text](/img/{17456DE9-0F67-42EF-8754-02F757926A37}.png)
#### 2.2.1. UserController.java 생성
```java
package com.ktds.eduuser.controller;

import com.ktds.eduuser.dto.response.GetUserResDto;
import com.ktds.eduuser.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.HttpServerErrorException;

@Slf4j
@RequiredArgsConstructor
@RequestMapping("/api/v1/user")
@RestController
public class UserController {
    private final UserService userService;

    @GetMapping("/{userNo}")
    public ResponseEntity<GetUserResDto> getUser(@PathVariable int userNo) {
        try {
            GetUserResDto getUserResDto = userService.getUser(userNo);
            return new ResponseEntity<>(getUserResDto, HttpStatus.OK);
        } catch (HttpServerErrorException e) {
            log.error(e.getMessage());
            return new ResponseEntity<>(HttpStatus.SERVICE_UNAVAILABLE);
        } catch (Exception e) {
            log.error(e.getMessage());
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
}
```
- [GoodsController](#211-goodscontroller-생성)와 흡사하나, path variable로 userNo를 받으면 return 값으로 userNo, userName, goodsNo, goodsName을 반환받도록 설계되었다.
- userNo와 동일한 goodsNo로 반환받도록 설계되었으며, edu-user 내부에서 edu-goods 인스턴스의 API를 호출하여 goods 정보를 받도록 한다.
- API를 동기식으로 내부에서 호출하는 과정에서, edu-goods 인스턴스에 도달하지 못할 경우 503 service unavailable 값을 받을 수 있도록 exception 처리를 수행한다.
#### 2.2.2. RestTemplate.java 생성
```java
package com.ktds.eduuser.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

@Configuration
public class RestTemplateConfig {

    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }
}
```
- edu-user 내부에서 edu-goods로의 동기식 API 통신을 위해 RestTemplate를 사용한다. 이를 위해 RestTemplate bean을 생성하여 활용한다.
#### 2.2.3. UserService.java 생성
```java
package com.ktds.eduuser.service;

import com.ktds.eduuser.dto.response.GetGoodsResDto;
import com.ktds.eduuser.dto.response.GetUserResDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestTemplate;

import java.util.Objects;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserService {

    @Value("${goods-url}")
    private String goodsUrl;

    @Value("${app.run.type}")
    private String env;

    private final RestTemplate restTemplate;

    public GetUserResDto getUser(int userNo) throws HttpServerErrorException {
        ResponseEntity<GetGoodsResDto> response = restTemplate.getForEntity(goodsUrl + userNo, GetGoodsResDto.class);
        log.debug("goods api call response http code: {}", response.getStatusCode());

        if (response.getStatusCode() == HttpStatusCode.valueOf(200)) {
            log.debug("goods api call succeeded.");
            return GetUserResDto.builder()
                    .userNo(userNo)
                    .userName(env + "-" + userNo)
                    .goodsNo(Objects.requireNonNull(response.getBody()).getGoodsNo())
                    .goodsName(response.getBody().getGoodsName())
                    .build();
        } else {
            log.debug("goods api call failed with http status code: {}", response.getStatusCode());
            throw new HttpServerErrorException(response.getStatusCode());
        }
    }
}
```
- edu-goods로 API 통신을 수행해야 하므로, edu-goods에 구현되어 있는 `/api/v1/goods/{goodsNo}`에 해당하는 endpoint를 `goodsUrl`로 받아올 수 있도록 환경 설정 파일로부터 불러오게끔 설계한다.
   - local, dev, prod 환경마다 goodsUrl을 분기처리 함으로써, local 테스트를 별도 수행을 한 후 k8s prod 환경에서 내부 FQDN(Fully Qualified Domain Names)을 통한 edu-goods pod 호출을 모두 수행할 수 있도록 한다.
- 동기식 API 호출 실패 시엔 503이 던져지도록 처리하고, 정상적인 수행일 시 200으로 던져지도록 로직을 설계한다.
#### 2.2.4. application properties 설정
```yaml
# application.yml
spring:
  application:
    name: edu-user
  profiles:
    active: loc

management: # 이후 liveness probe 구현을 위한 health checking 용 spring actuator 설정값
  endpoints:
    web:
      exposure:
        include: health
  endpoint:
    health:
      probes:
        enabled: true

# application-loc.yml
app:
  run:
    type: local

server:
  port: 8080

goods-url: http://localhost:8081/api/v1/goods/ # 로컬 환경에서 띄운 edu-goods 인스턴스 호출 url

logging:
  level:
    com.ktds: DEBUG

# application-prod.yml
app:
  run:
    type: prod

#goods-url: http://edu-goods.211.254.213.33.sslip.io/api/v1/goods/ # 상용 환경에서 띄운 edu-goods external ip 호출 url
goods-url: http://edu-goods-svc.edu-goods.svc.cluster.local/api/v1/goods/ # 상용 환경에서 띄운 edu-goods FQDN 내부 호출 url

logging:
  level:
    com.ktds: DEBUG

# application-dev.yml 생략
```
- 상용 환경에서 ingress 설정을 통해 `http://edu-goods.211.254.213.33.sslip.io/api/v1/goods/` 외부 경로 호출이 가능하나, 요구사항 중 FQDN을 통해 내부 클러스터 경로로 잡는 것이 목표이기 때문에 FQDN인 `http://edu-goods-svc.edu-goods.svc.cluster.local/api/v1/goods/`을 사용한다.
   - FQDN 포맷 형식: `<service-name>.<namespace>.svc.cluster.local`

### 2.3. 각 서비스 도커라이징
```Dockerfile
FROM eclipse-temurin:21-jre-ubi9-minimal

ARG JAR_FILE=build/libs/*.jar

COPY ${JAR_FILE} app.jar

ENTRYPOINT ["java", "-jar", "/app.jar"]
```
- edu-user, edu-goods의 경우 JAVA 21 환경으로 구축하였기 때문에 JAVA 21 JRE 기반 이미지를 사용.
   - 이미 빌드된 jar 파일을 복사하여 실행만 시킬 것이기 때문에, JRE를 사용하였으며 minimal 버전을 사용하여 용량을 최소화.
- 해당 Dockerfile을 기반으로 docker image를 생성하기 전, `./gradlew bootJar` 명령어를 통해 Jar 파일을 빌드해 두어야 한다.
- gradle build를 하였기 때문에 빌드 완료된 Jar 파일의 경우 `/build/libs` 경로에 있음을 유의하여 `JAR_FILE` 경로를 설정한다.
- MacOS에 설치된 docker desktop을 이용중일 경우, Mac 실리콘 칩 기반일 시 동일한 `docker build` 명령어를 사용하면 실리콘 칩 호환 빌드가 이루어진다.
   - 해당 image 파일을 사용하여 amd64 기반 칩셋 위에 구동중인 linux나 windows 위에서 실행할 시 호환성 오류가 나므로, MacOS 내에서 호환되도록 빌드를 수행 시 `docker buildx build --platform linux/amd64 -t <tag-name> -f <Dockerfile-location> <path>` 구조로 플래그 명령어 사용을 통해 빌드해야함에 유의.
```sh
docker build -t xronace/edu-user:v0.0.1 -f Dockerfile .
docker build -t xronace/edu-goods:v0.0.1 -f Dockerfile .
```

### 2.4. 각 서비스 도커 이미지를 docker hub에 업로드
- `docker login`을 통해 dockerhub 계정에 로그인 한다.
- `docker push <repo-name>/<image-name>:<version>` 구조로 빌드 완료된 image를 업로드한다.
```sh
docker login
docker push xronace/edu-user:v0.0.1
docker push xronace/edu-goods:v0.0.1
```

### 2.5. 각 서비스 deployment, service, ingress 생성
#### 2.5.1. edu-goods-deploy.yaml
```yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: edu-goods-deploy
  namespace: edu-goods
spec:
  selector:
    matchLabels:
      app: edu-goods-app
  replicas: 1
  template:
    metadata:
      labels:
        app: edu-goods-app
        version: blue
    spec:
      containers:
        - name: edu-goods-app
          image: xronace/edu-goods:v0.0.1      # dockerhub image repository : repo/image명:Tag
          imagePullPolicy: IfNotPresent        # k8s 클러스터에 다운로드 된 이미지 없으면 다운 or Always
          ports:
            - name: http
              containerPort: 8081              # edu-goods.jar 실행 포트
              protocol: TCP
          resources:                           # pod 사용 리소스 설정 블록
            requests:                          # 생성시 필요한 최소 리소스
              cpu: "1"
              memory: "2Gi"
            limits:                            # pod가 사용 가능한 최대 리소스
              cpu: "1"
              memory: "2Gi"
          env:                                 # pod 내 환경 변수 설정
            - name: SPRING_PROFILES_ACTIVE     # spring profile 설정
              value: prod                      # 상용(prod) 환경
          livenessProbe:
            httpGet:
              path: /actuator/health
              port: 8081
            initialDelaySeconds: 30
            periodSeconds: 5
            timeoutSeconds: 1
            failureThreshold: 3
      imagePullSecrets:                        # dockerhub 이미지 pull 위한 secret
        - name: k8s-edu-dockerhub-secret

---
apiVersion: v1
kind: Service
metadata:
  name: edu-goods-svc
  namespace: edu-goods
spec:
  selector:
    app: edu-goods-app
    version: blue  
  ports:
    - protocol: TCP
      port: 80
      targetPort: 8081                        # deployment에서 설정한 컨테이너 포트 매핑

---
apiVersion: networking.k8s.io/v1
kind: Ingress
metadata:
  name: edu-goods-ing
  namespace: edu-goods
spec:
  ingressClassName: nginx
  rules:
    - host: "edu-goods.211.254.213.33.sslip.io"
      http:
        paths:
          - path: /
            pathType: Prefix
            backend:
              service:
                name: edu-goods-svc
                port:
                  number: 80
```

### 2.6. argoCD를 활용하여 배포
#### 2.6.1. 인스턴스 별 manifests 생성
- edu-goods  
![alt text](/img/image-2.png)
![alt text](/img/image-3.png)
#### 2.6.2. argoCD에 application 생성
![alt text](/img/image-4.png)
#### 2.6.3. Synchronize 후 Pod 생성 확인
- edu-goods: min 1pod, max 2pods autoscaled  
![alt text](/img/image-5.png)
- edu-user: min 2pods, max 4pods autoscaled  
![alt text](/img/image-6.png)

### 2.7. 배포된 형상에서 인스턴스 별 API 호출 테스트 및 동작 확인
#### 2.7.1. edu-goods 외부 링크로 API 호출
- [edu-goods.211.254.213.33.sslip.io/api/v1/goods/82273950](http://edu-goods.211.254.213.33.sslip.io/api/v1/goods/82273950)
![alt text](/img/image.png)

#### 2.7.2. edu-user 외부 링크로 API 호출
- [http://edu-user.211.254.213.33.sslip.io/api/v1/user/82273950](http://edu-user.211.254.213.33.sslip.io/api/v1/user/82273950)
![alt text](/img/image-1.png)
- edu-user의 API 내부 로직에서는 FQDN을 통해 edu-goods를 내부 네트워크로 접근하여 호출.

## 3. HPA 설정 : edu-user(min:2, max:4), edu-goods(min:2, max:2)
### 3.1. edu-user HPA autoscaling
```yaml
apiVersion: autoscaling/v2
kind: HorizontalPodAutoscaler
metadata:
  name: edu-user-hpa
  namespace: edu-user
spec:
  scaleTargetRef:
    apiVersion: apps/v1
    kind: Deployment
    name: edu-user-deploy
  minReplicas: 2
  maxReplicas: 4
  metrics:
    - type: Resource
      resource:
        name: cpu
        target:
          type: Utilization
          averageUtilization: 30
```
- CPU 평균 사용량이 30%를 초과할 시, scale-up 수행하고, 기본 설정값인 300초 동안 CPU 평균 사용량이 30% 이하일 시 scale down 된다.
### 3.2. edu-goods HPA autoscaling
```yaml
apiVersion: autoscaling/v2
kind: HorizontalPodAutoscaler
metadata:
  name: edu-goods-hpa
  namespace: edu-goods
spec:
  scaleTargetRef:
    apiVersion: apps/v1
    kind: Deployment
    name: edu-goods-deploy
  minReplicas: 1
  maxReplicas: 2
  metrics:
    - type: Resource
      resource:
        name: cpu
        target:
          type: Utilization
          averageUtilization: 30
```
- 마찬가지로 CPU 평균 사용량 30% 기준으로 scaling이 이루어진다.
- 기본적으로 HPA는 rke2에 기본적으로 설치된 metrics를 기반으로 하드웨어 리소스 점유율을 추적하여 작동한다.

## 4. probe 설정하여 pod 내 어플리케이션이 다운 되면 자동으로 pod start하게 설정(health check, self-healing)
### 4.1. 각 애플리케이션에 health check용 API endpoint 구축
- 편의를 위해 Spring의 actuator를 사용하고, CustomHealthIndicator를 구현하여 log를 남기도록 처리함으로써 작동 여부를 로그로 확인할 수 있도록 한다.
```java
package com.ktds.eduuser.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class CustomHealthIndicator implements HealthIndicator {

    @Override
    public Health health() {
        log.debug("Health check called.");
        return Health.up().build();
    }
}
```
- actuator 기본 내장 health API endpoint를 활용하되, 로그를 남기도록 커스터마이즈 진행.
### 4.2. Spring actuator의 health API endpoint로 liveness Probe 설정
- deployment.yaml 내에 liveness Probe 값을 설정한다.
```yaml
livenessProbe:
  httpGet:
    path: /actuator/health
    port: 8080
  initialDelaySeconds: 30
  periodSeconds: 5
  timeoutSeconds: 1
  failureThreshold: 3
```
- actuator 기본 내장 health API 경로는 /actuator/health이다.
- initialDelaySeconds를 30초를 줌으로써 pod가 실행되어 pod 내 container로 애플리케이션이 완전히 작동할 때 까지 30초의 텀을 제공한 후, health check가 처리되도록 설정한다.
### 4.3. actuator의 내장 health 실행을 위한 spring application.yml 편집
```yaml
management:
  endpoints:
    web:
      exposure:
        include: health
  endpoint:
    health:
      probes:
        enabled: true
```
- 해당 환경변수 설정을 통해 actuator의 health endpoint를 노출시킨다.
### 4.4. 실제 health check 실행 여부를 pod 로그에서 확인
![alt text](/img/image-7.png)
- 설정했던 5초 주기로 health check를 하는 모습을 확인할 수 있다.