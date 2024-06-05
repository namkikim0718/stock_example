## 목차
1. [개발 환경](#개발-환경)
2. [작업 환경 세팅](#작업-환경-세팅)
3. [문제 상황](#문제-상황)
4. [해결 방법](#해결-방법)
    1. [Java synchronized](#1-Java의-synchronized를-이용한-방법)
    2. [Pessimistic Lock](#2-Pessimistic-Lock(비관적-락))
    3. [Optimistic Lock](#3-Optimistic-Lock(낙관적-락))
    4. [Named Lock](#4-Named-Lock)
    5. [Lettuce](#5-Lettuce)
    6. [Redisson](#6-Redisson)
5. [MySQL vs Redis](#MySQL-vs-Redis)
</br>

## 개발 환경
- 기본 환경
  - IDE: IntelliJ IDEA
  - OS: Mac OS Sonoma
  - Git
- Server
  - Java17
  - Spring Boot 3.2.4
  - JPA
  - Docker
  - Redis
  - MySQL
  - Gradle
  - Junit5
 
</br>
 
## 작업 환경 세팅
- Docker 설치</br>
```
brew install docker
brew link docker
docker version
```
</br>

- MySQL 설치 및 실행
```
docker pull mysql
docker run -d -p 3307:3306 -e MYSQL_ROOT_PASSWORD=1234 --name mysql mysql
docker ps
```
> port가 겹쳐서 3307로 설정(겹치지 않으면 3306:3306 으로 해도 무방)

</br>

- MySQL 데이터베이스 생성
```
docker exec -it mysql bash
mysql -u root -p
create database stock_example;
use stock_example;
```

</br>

- Redis 설치 및 실행
```
docker pull redis
docker run --name myredis -d -p 6379:6379 redis
```
</br>

- Redis 의존성 추가
```
implementation 'org.springframework.boot:spring-boot-starter-data-redis'
```
</br>

- redisson 의존성 추가
```
implementation 'org.redisson:redisson-spring-boot-starter:3.27.2'
```
> Maven Repository에서 redisson 검색 후 최신 버전으로 주입

</br>

## 문제 상황
- 우선, 기존에는 재고를 감소하는데 항상 1개의 요청씩만 들어온다는 가정하에 코드를 작성
- 동시에 100개의 요청이 들어오게 되면 어떻게 동작할 지 테스트 코드로 구현해봤으나, 100개의 요청이 전부 실행되는 것이 아니라 누락이 발생하는 것을 확인
- 확인해보니 공통 자원을 병행하여 읽거나 쓰는 작업을 하면서 생기는 `Race Condition`이 원인임을 발견
  - [Race Condition이란?](https://iredays.tistory.com/125)
- 그렇다면 이 문제를 어떤 방식으로 해결할 수 있을까?
</br>

## 해결 방법
### 1. Java의 synchronized를 이용한 방법
- 메서드 반환 타입 앞에 `synchronized`를 추가 -> `@Transactional`을 없애야만 정상적으로 동작
- 한계점 : 한개의 서버에서만 동시성을 보장하기 때문에, 서버가 2개 이상이라면 또다시 `Race Condition` 발생  

### 2. Pessimistic Lock(비관적 락)
- 장점
  - 충돌이 빈번하게 일어날 수록 낙관적 락보다 좋은 성능을 보여줌
  - DB단에서 Lock을 통해 동시성을 제어하므로 데이터 정합성이 확실하게 보장됨
- 단점
  - DB단에서 Lock을 설정하므로 한 트랜잭션 작업이 정상적으로 끝나지 않으면 다른 트랜잭션들이 대기해야 하므로 성능이 감소할 수 있음  

### 3. Optimistic Lock(낙관적 락)
- 장점
  - 비관적 락과 달리 별도의 DB단의 Lock을 설정하지 않고 Application Level에서 Lock을 설정하므로, 하나의 트랜잭션 작업이 길어져도 다른 작업은 영향받지 않아 성능이 좋을 수 있음
- 단점
  - 버전이 맞지 않아 예외가 발생할 때 재시도 로직을 직접 구현해야함
  - 충돌이 여러번 발생하면 재시도 횟수가 늘어나므로 성능 저하
 </br>
 
`Pessimistic Lock` vs `Optimistic Lock`

![비관적 락 vs 낙관적 락](https://github.com/namkikim0718/stock_example/assets/113903598/63555d40-c56b-420e-90e7-d5f3df0ed8d3)

</br>
  
### 4. Named Lock 
- 장점
  - Lock을 DB객체 자체에 설정하는 것이 아니라, 별도의 공간에 Lock을 걸게 되므로 Named Lock을 사용하는 작업 이외의 작업이 영향을 받지 않음
  - UPDATE 가 아닌 INSERT 작업에서는 기준을 잡을 레코드가 존재하지 않아 비관적 락을 사용할 수 없으나, Named Lock은 사용할 수 있음
  - 분산 락 구현 가능
- 단점
  - 트랜잭션 종료 시 Lock해제와 세션 관리를 직접 처리해야 하므로 구현이 복잡할 수 있음
> 실무에서는 Data Source를 분리해 사용하는 것이 좋음
</br>

### 5. Lettuce
- 장점
  - spring data redis를 사용하면 lettuce가 기본이므로 별도의 라이브러리 사용하지 않고 간단하게 구현 가능
- 단점
  - spin lock 방식이므로 lock이 해제 되었는지 주기적으로 재시도를 하면서 부하가 커질 수 있음
  - 분산 락 기능을 제공하지 않다 직접 구현해야함

</br>

### 6. Redisson
- 장점
  - 분산 락 기능을 제공해 별도의 구현이 필요없음
  - pub-sub 방식으로 구현되어 있어 lock이 해제되면 대기중인 스레드들에게 알려주는 구조이므로 부하가 적음
- 단점
  - 별도의 라이브러리를 사용해야함
  - lock을 별도의 라이브러리에서 제공해주므로 사용법을 공부해야함
> 실무에서는 재시도가 필요하지 않은 lock은 `lettuce` 활용  
> 재시도가 필요한 경우에 `redisson` 활용

</br>

## MySQL vs Redis
- MySQL
  - MySQL만 있다면 별도의 비용없이 사용 가능
  - 트래픽이 아주 많은 것이 아니라면 MySQL로도 문제없이 활용 가능
  - 그러나 Redis 보다 성능은 좋지 않음

</br>

- Redis
  - 이미 사용중인 Redis가 존재하지 않으면 별도의 구축 비용과 인프라 관리비용이 발생
  - 트래픽이 많은 서비스일수록 MySQL보다 성능이 좋음

</br>

## k6를 이용한 테스트
- 비관적 락
<img width="956" alt="스크린샷 2024-06-03 09 39 45" src="https://github.com/namkikim0718/stock_example/assets/113903598/310d482c-bddc-41fb-bb63-060e42634495">

- 낙관적 락 
<img width="906" alt="스크린샷 2024-06-03 09 42 26" src="https://github.com/namkikim0718/stock_example/assets/113903598/d67c56b7-02bd-4035-acbf-fa2232e58137">


- 네임드 락
<img width="907" alt="스크린샷 2024-06-03 09 43 27" src="https://github.com/namkikim0718/stock_example/assets/113903598/5440d9d4-045e-4a27-a456-a70edd1bd1e3">


- lettuce
<img width="922" alt="스크린샷 2024-06-03 09 44 48" src="https://github.com/namkikim0718/stock_example/assets/113903598/588dc5dc-56dd-4b83-80e3-06e080ab79a2">


- redisson
<img width="924" alt="스크린샷 2024-06-03 09 46 00" src="https://github.com/namkikim0718/stock_example/assets/113903598/eba4da9c-1edd-4a40-bddd-c6bd132367d9">

