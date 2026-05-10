# Chapter08. 프록시와 연관관계 매핑
## 1. 프록시

상황 예시
<img width="439" height="105" alt="image" src="https://github.com/user-attachments/assets/f1dcc6c1-ed93-4688-8d4c-21061fb5ebfa" />


Member를 조회할 때 Team도 함께 조회?

어느 경우에는 Member와 Team 같이 가져오고 싶고 어느 경우에는 Member만 가져오고 싶음 (최적화위해!)

#### 회원과 팀 함께 출력
```java
public void printUserAndTeam(String memberId) {
    Member member = em.find(Member.class, memberId);
    Team team = member.getTeam();
    System.out.println("회원 이름: " + member.getUsername());
    System.out.println("소속팀: " + team.getName());
}
```
- member.getTeam() 을 통해 Team까지 실제 접근함 → Team 조회 SQL 발생
- 이 경우 `team.getName()` 호출 시점에 프록시 초기화 (지연 로딩 시)

#### 회원만 출력
```java
public void printUser(String memberId) {
    Member member = em.find(Member.class, memberId);
    Team team = member.getTeam();
    System.out.println("회원 이름: " + member.getUsername());
}
```
- `member.getTeam()`까지만 호출하므로 Team 엔티티에 접근하지 않음
- 이때는 Team은 프록시로 존재할 수 있고, 초기화되지 않음

### 1.1 프록시 개념

#### em.find() vs em.getReference()
|메서드|설명|
|:---|:---|
|em.find()|데이터베이스에 즉시 접근하여 실제 엔티티 객체 조회|
|em.getReference()|데이터베이스 조회를 지연시키는 프록시 객체(가짜)를 반환|

#### 프록시 동작 구조
<img width="580" height="116" alt="image" src="https://github.com/user-attachments/assets/6b063081-c4dd-4e80-9992-6c7b04bb25ea" />

- `getReference()` 호출 시 DB 접근 없이 프록시 객체 생성
- 프록시 객체는 실제 객체를 참조(target) 하지 않고, 내부는 비어 있음 (Entity target = null)
- 프록시 객체의 메서드 호출 시 실제 DB 접근하여 초기화됨
예: `proxy.getName()` 호출 → 이때 DB 조회 수행

DB에 쿼리가 안나가는데 객체가 조회됨!

### 1.2 프록시 특징
#### 프록시 클래스 구조

<img width="139" height="251" alt="image" src="https://github.com/user-attachments/assets/a0edb43a-82ca-4341-a6be-8236ffe85a9c" />

- 프록시 객체는 실제 엔티티 클래스를 상속해서 만들어짐
- 따라서 실제 클래스와 겉모습이 동일
- 사용하는 입장에서는 진짜 객체인지 
- → 이론적으로는 프록시인지 진짜 객체인지 구분 없이 사용 가능
#### 프록시의 동작 원리
<img width="379" height="131" alt="image" src="https://github.com/user-attachments/assets/a981ad5e-b129-486e-aa87-7a3323dd1010" />

- 프록시 내부에 실제 객체 참조(target)를 가지고 있음
- 프록시 객체를 호출하면 → 프록시 객체는 실제 객체의 메소드를 위임 호출

근데 처음에 DB에서 조회한 적이 없으니까 실제target 이 없겠지? → 프록시 객체의 초기화

### 1.3 프록시 객체의 초기화
#### 프록시 객체의 초기화 과정
<img width="614" height="338" alt="image" src="https://github.com/user-attachments/assets/692158ac-b6e0-47e6-a9e0-c5f47e38a177" />

```java
Member member = em.getReference(Member.class, "id1");
member.getName(); // 실제 접근
```
1. `getReference()` 호출 → 프록시 객체 반환
2. `member.getName()` 호출 시 프록시가 초기화 요청
3. 영속성 컨텍스트가 DB에 실제 데이터를 조회
4. 조회된 데이터로 실제 엔티티 생성
5. 프록시가 target.getName() 호출로 위임

#### 프록시의 특징 정리
- 초기화 : 프록시는 처음 사용 시 단 한번만 초기화됨
- 프록시 자체는 그대로 : 초기화되어도 프록시 객체는 실제 객체로 바뀌지 않음, 내부에서 실제 객체에 접근
- 상속 구조 : 프록시는 실제 엔티티를 상속하므로 타입 체크 시 주의 !! (`==` 비교는 실패할 수 있음 → `instanceof` 사용)
- 영속성 컨텍스트 확인 : 이미 영속성 컨텍스트에 있으면 `getReference()`도 실제 엔티티 반환

```java
Member refMember = em.getReference(Member.class, member1.getId());
System.out.println("refMember = " + refMember.getClass()); //Proxy

Member findMember = em.find(Member.class, member1,getId());
System.out.println("findMember = " + findMember.getClass()); //Member

System.out.println("refMember == findMember: " + (refMember == findMember));
```
findMember도 froxy가 반환돼서 true가 출력된다.
JPA에서든 ref == get을 맞춘다고 생각..

- 준영속 상태 시 문제 : 영속성 컨텍스트에서 관리되지 않으면 초기화 시 org.hibernate.LazyInitializationException 예외 발생

```java
Member refMember = em.getReference(Member.class, member1.getId());
System.out.println("refMember = " + refMember.getClass()); //Proxy
            
//em.close(); 또는 em.detach(); 를 해버리면 영속성 컨텍스트를 더이상 관리 안해! 라고 하는 거이기 때문에 프록시를 초기화할 수 없다고 뜬다.

System.out.println("refMember = " + refMember.getUsername()); //이때 실제 DB의 쿼리가 나가면서 프록시 객체가 초기화됨
```

사실 getReference를 실제 많이 사용하지는 X

#### 프록시 확인
- 프록시 초기화 여부 확인
`PersistenceUnitUtil.isLoaded(Object entity)` → 해당 엔티티가 초기화되었는지 여부 확인

- 프록시 클래스 확인
`entity.getClass().getName()` → 결과에 javassist, HibernateProxy 등이 포함되어 있으면 프록시 객체

- 프록시 강제 초기화
`org.hibernate.Hibernate.initialize(entity);` → 강제로 DB 접근하여 프록시 초기화
*JPA 표준에는 강제 초기화 API가 없음

참고) 프록시 강제 초기화 없이도 member.getName() 처럼 실제 데이터를 접근하면 자동 초기화됨

---

## 2. 즉시 지연과 지연 로딩
### 2.1 지연 로딩
상황 설명

Member를 조회할 때 Team도 함께 조회해야 할까?

단순히 Member의 정보만 필요한 비즈니스 로직이라면 → 굳이 Team을 즉시 로딩할 필요 없음

System.out.println(member.getName()); // Team은 사용하지 않음

#### 해결 방법: 지연 로딩 설정
`fetch = FetchType.LAZY`
```java
@Entity
public class Member {

    @Id
    @GeneratedValue
    private Long id;

    @Column(name = "USERNAME")
    private String name;

    @ManyToOne(fetch = FetchType.LAZY) // 지연 로딩 설정
    @JoinColumn(name = "TEAM_ID")
    private Team team;

    ..
}
```

사용 시점에만 조회되는 LAZY 전략은 성능 최적화에 매우 중요 !

#### 지연 로딩(LAZY)의 동작 흐름
<img width="466" height="138" alt="image" src="https://github.com/user-attachments/assets/2f5629cb-62d3-4cef-906e-76ea1e3305eb" />


1.`Member1` 로딩 시점

Member member = em.find(Member.class, 1L);

- `member` 엔티티만 실제 객체로 로딩됨
- `team`은 프록시 객체로 로딩됨 → DB 접근 없음

2.`Team` 접근 시점

Team team = member.getTeam();  // 아직은 프록시 상태

team.getName();  // 이 시점에 실제 DB 조회

- 프록시 객체가 초기화됨 (DB 조회 발생)
- 이후부터는 `team` 프록시가 내부적으로 실제 Team 엔티티 참조
    
### 2.2 즉시 로딩
상황 설명

Member와 Team을 자주 함께 사용한다면?

Member를 조회할 때 Team도 항상 필요하다면 → 즉시 로딩(EAGER) 으로 연관된 Team까지 자동 조회

#### EAGER 로딩 설정 예제
`fetch = FetchType.EAGER`
```java
@Entity
public class Member {

    @Id
    @GeneratedValue
    private Long id;

    @Column(name = "USERNAME")
    private String name;

    @ManyToOne(fetch = FetchType.EAGER) // 즉시 로딩 설정
    @JoinColumn(name = "TEAM_ID")
    private Team team;

    ..
}
```
em.find(Member.class, id) 호출 시

Member와 Team을 함께 SQL로 조회 (JOIN 쿼리 또는 별도 쿼리)

#### 즉시 로딩(EAGER) 동작 흐름
<img width="463" height="142" alt="image" src="https://github.com/user-attachments/assets/b5a38688-fc48-4455-884b-661cda936ba8" />

1.`Member`를 `em.find()`로 조회

2.`@ManyToOne(fetch = FetchType.EAGER)` 설정 덕분에 연관된 `Team`도 즉시 함께 조회

#### 프록시와 즉시로딩 주의
실무에서는 가급적 지연 로딩(LAZY)만 사용
- 불필요한 쿼리 방지
- 필요할 때만 DB 접근 → 성능 최적화

즉시 로딩 사용 시 주의
- JPQL 사용 시 예상하지 못한 SQL 발생
- N+1 문제 유발 가능성 큼

연관관계 기본 fetch 전략

|어노테이션|기본 전략|실무 권장|
|:---|:---|:---|
|@ManyToOne, @OneToOne|즉시 로딩(EAGER)|fetch = LAZY로 설정 필요|
|@OneToMany, @ManyToMany|지연 로딩(LAZY)|기본값 그대로 사용 가능|

### 2.3 관계별 로딩 전략
<img width="324" height="175" alt="image" src="https://github.com/user-attachments/assets/727a3cbf-05ea-4765-a152-8f4c4a640c1d" />

|관계|사용 빈도|추천 로딩 전략|
|:---|:---|:---|
|Member ↔ Team|자주 사용|즉시 로딩 (EAGER)|
|Member ↔ Order|가끔 사용|지연 로딩 (LAZY)|
|Order ↔ Product|자주 사용|즉시 로딩 (EAGER)|

<img width="514" height="272" alt="image" src="https://github.com/user-attachments/assets/f852ddbc-71ac-4bdb-b59f-bfd368f44b59" />

<img width="522" height="292" alt="image" src="https://github.com/user-attachments/assets/6784c376-4ef2-4d97-8ae8-c177087da740" />


#### 지연 로딩 활용 - 실무 전략
모든 연관관계에 지연 로딩을 사용
- 실무에서는 기본적으로 FetchType.LAZY 적용을 권장
- 불필요한 조인을 피하고 성능 문제 방지

실무에서 즉시 로딩(EAGER) 사용 X
- 즉시 로딩은 예측 불가능한 SQL, 조인 쿼리 폭발 가능성 있음
- 특히 JPQL 사용 시 N+1 문제 쉽게 발생

해결 방법
- 필요한 경우에만 JPQL fetch join
- 또는 EntityGraph 기능으로 명시적 제어

즉시 로딩은 상상하지 못한 쿼리가 나간다
- 예: JPQL에서는 지정하지 않아도 EAGER 관계가 강제로 JOIN 됨

---

## 3. 영속성 전이 CASCADE
### 3.1 영속성 전이
부모 엔티티를 영속화할 때, 연관된 자식 엔티티도 자동으로 영속화되는 기능

즉, `em.persist(parent)` 만으로 연관된 child 엔티티들도 자동 저장

#### 영속성 전이: 저장
```java
@OneToMany(mappedBy = "parent", cascade = CascadeType.PERSIST)
private List<Child> children;
```
<img width="450" height="177" alt="image" src="https://github.com/user-attachments/assets/63b5c0ce-be2d-47ac-966c-27df72a60fb1" />

예시 상황
1. `Parent` 엔티티를 생성하고 자식 Child들을 리스트에 추가
2. `em.persist(parent)` 호출 시
3. `child1`, `child2`도 함께 자동으로 DB에 INSERT

#### 영속성 전이: 주의사항
영속성 전이는 연관관계 매핑과는 무관
- `@OneToMany` 또는 `@ManyToOne` 같은 연관관계 설정과는 전혀 다른 개념

단지 영속화(PERSIST), 삭제(REMOVE) 등의 작업을 편리하게 전달하는 도구일 뿐
- `em.persist(parent)` 시 → 자식들도 자동 `persist` 되는 편의 기능

#### CASCADE의 종류
|옵션|설명|
|:---|:---|
|ALL|	모든 전이 동작 적용 (PERSIST, REMOVE, MERGE, REFRESH, DETACH)|
|PERSIST|엔티티 저장 전이|
|REMOVE|엔티티 삭제 전이|
|MERGE|	병합 (수정) 전이|
|REFRESH|	DB 상태로 다시 읽어올 때 전이|
|DETACH|	영속성 컨텍스트에서 분리할 때 전이|


### 3.2 고아 객체
#### 고아 객체란?
부모 엔티티와의 연관관계가 끊긴 자식 엔티티를 자동으로 삭제하는 기능
```java
@OneToMany(mappedBy = "parent", orphanRemoval = true)
private List<Child> children;
```

예시
```java
Parent parent1 = em.find(Parent.class, id);
parent1.getChildren().remove(0);  // 자식 리스트에서 제거
```
→ 해당 자식은 DELETE 쿼리로 자동 삭제됨

#### 고아 객체 기능 사용 시 주의사항
- 참조가 하나일 때만 사용!! 자식 엔티티가 여러 곳에서 참조되면 의도치 않게 삭제될 수 있음
- 개인 소유 관계에만 사용 : 특정 부모가 해당 자식을 독점 소유할 때만 사용
- 적용 가능한 연관관계 : `@OneToOne`, `@OneToMany` 에서만 사용 가능

*참고
부모가 삭제되면 자식은 고아가 되고, orphanRemoval = true일 경우 자동으로 삭제됨

이는 CascadeType.REMOVE와 유사하게 작동

### 3-3. 영속성 전이 + 고아 객체, 생명주기
```java
cascade = CascadeType.ALL
orphanRemoval = true
```

|항목|설명|
|:---|:---|
|CascadeType.ALL|부모가 persist/remove되면 자식도 자동 반영|
|orphanRemoval=true|부모와의 연관이 끊긴 자식은 자동 삭제|
|두 옵션 동시 사용 시|부모가 자식의 전체 생명주기를 관리 가능|
|활용 시점|도메인 주도 설계(DDD)의 Aggregate Root 패턴에 적합|

예시
```java
Order order = new Order();
order.addOrderItem(new OrderItem());
em.persist(order);  // → OrderItem도 함께 저장됨

order.removeOrderItem(orderItem);  
// → 연관 제거 → 자동 삭제 (orphanRemoval)
```

즉, 부모가 자식을 생성부터 삭제까지 전담할 때 이 조합을 사용, 연관관계를 안정적으로 관리하고, 코드의 명시성도 높아짐
