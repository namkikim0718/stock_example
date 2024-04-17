## 문제 상황
- 우선, 기존에는 재고를 감소하는데 항상 1개의 요청씩만 들어온다는 가정하에 코드를 작성
- 동시에 100개의 요청이 들어오게 되면 어떻게 동작할 지 테스트 코드로 구현해봤으나, 100개의 요청이 전부 실행되는 것이 아니라 누락이 발생하는 것을 확인
- 확인해보니 공통 자원을 병행하여 읽거나 쓰는 작업을 하면서 생기는 `Race Condition`이 원인임을 발견
  - [Race Condition이란?](https://iredays.tistory.com/125)
- 그렇다면 이 문제를 어떤 방식으로 해결할 수 있을까?


## 해결 방법
#### 1. Java의 synchronized를 이용한 방법
- 메서드 반환 타입 앞에 `synchronized`를 추가 -> `@Transactional`을 없애야만 정상적으로 동작
- 한계점 : 서버가 1개라면 잘 동작하지만, 서버가 2개 이상이라면 각각의 서버에서 동시에 접근하면 또다시 `Race Condition` 발생


#### 2. DB Lock을 이용한 방법
1. Pessimistic Lock(비관적 락)
- 트랜잭션끼리의 충돌이 발생한다고 가정하고 우선 락을 거는 방법
- 해당 트랜잭션이 종료되기 전에 다른 트랜잭션에서는 해당 row를 업데이트 할 수 없음
2. Optimistic Lock(낙관적 락)
3. 
