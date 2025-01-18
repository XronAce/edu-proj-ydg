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
### 2-1. edu-user 서비스용 인스턴스 개발
### 2-2. edu-goods 서비스용 인스턴스 개발
### 2-3. 각 서비스 도커라이징
### 2-4. 각 서비스 도커 이미지를 docker hub에 업로드
### 2-5. 각 서비스 deployment.yaml 생성
### 2-6. 각 서비스 service.yaml 생성
### 2-7. argoCD를 활용하여 배포
### 2-8. 배포된 형상에서 인스턴스 별 API 호출 테스트 및 동작 확인
### 2-9. istio sidecar injection을 통해 flow graph 기반으로 API 통신 과정 시각적 확인

## 3. HPA 설정 : edu-user(min:2, max:4), edu-goods(min:2, max:2)

## 4. probe 설정하여 pod 내 어플리케이션이 다운 되면 자동으로 pod start하게 설정(health check, self-healing)