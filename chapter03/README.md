# Chpater03. 영속성 관리
## 1. 영속성 컨텍스트

### 1.1 영속성 컨텍스트
- 엔티티를 영구 저장하는 환경
- `EntityManager.persist(entity);` : 사실 DB에 저장하는 게 아니라 엔티티를 영속성 컨텍스트라는 데에 저장한다는 의미
- 엔티티 매니저를 통해 영속성 컨텍스트에 접근

#### 엔티티 매니저 팩토리와 엔티티 매니저
요청이 올 때마다 엔티티 매니저 팩토리는 엔티티 매니저를 생성한다. 그러면 엔티티 매니저는 내부적으로 데이터베이스 커넥션을 사용해서 DB를 사용하게 된다.

<img width="454" height="275" alt="image" src="https://github.com/user-attachments/assets/528e13b1-92f6-4a7e-a8fd-d3f3c8b76b87" />

### 1.2 엔티티의 생명주기
- 비영속 (new/transient): 영속성 컨텍스트와 전혀 관계가 없는 새로운 상태(생성만 한 상태)
- 영속 (managed): 영속성 컨텍스트에 관리되는 상태 (entity.persist())
- 준영속 (detached): 영속성 컨텍스트에 저장되었다가 분리된 상태. 영속성 컨텍스트에서 다시 지운다.
- 삭제 (removed): 삭제된 상태. DB 삭제를 요청하는 상태

```java
// 객체를 생성한 상태(비영속)
Member member =  new Member();
member.setId("member1");
member.serUsername("회원1");

EntityManaer em = emf.createEntityManager();
em.getTransaction().begin();

// 객체를 저장한 상태(영속) -> 아직 DB에 저장은 X
em.persist(member);

// 회원 엔티티를 영속성 컨텍스트에서 분리. 준영속 상태
em.detach(member);

// 객제를 삭제한 상태(삭제)
em.remove(member);
```

트랜잭션을 커밋하는 시점에 영속성 컨텍스트에 있는 DB의 쿼리가 날라간다!

### 1.3 영속성 컨텍스트의 이점
애플리케이션이랑 데이터베이스 사이의 중간 계층에서
- 1차 캐시
- 동일성(identity) 보장
- 트랜잭션을 지원하는 쓰기 지연
- 변경 감지(Dirty Checking)
- 지연 로딩(Lazy Loading)

#### - 엔티티 조회, 1차 캐시
```java
Member member = new Member();
member.setId("member1");
member.setUsername("회원1");

// 1차 캐시에 저장됨
em.persist(member);

// 1차 캐시에서 조회
Member fineMember = em.find(Member.class, "member1");
```
코드 실행해보면 쿼리 안나가는 걸 볼 수 있다.

1차 캐시에 없네? → db에서 조회 → db에서 조회해서 1차 캐시에 저장 → 이후에는 1차 캐시에서 조회 가능!

근데 트랜잭션 끝날 때 1차 캐시도 다 날라가기 때문에 큰 성능 이점은 없다.

#### - 영속 엔티티의 동일성 보장
```java
Member a = em.find(Member.class, "member1");
Member b = em.find(Member.class, "member2");

System.out.println(a == b); //동일성 비교 true
```
JPA에서 똑같은 거 같은 트랜잭션 안에서 실행(비교)했을 때 동일성 보장 true

#### - 엔티티 등록할 때 트랜잭션을 지원하는 쓰기 지연
persist 할 때 INSERT SQL을 DB에 보내는 게 아니라, transaction.commit 하는 순간에 쿼리 보냄!

→ 즉, persist 할 때마다 쓰기지연 SQL 저장소에 쌓아두었다가, 트랜잭션을 커밋하는 시점에 플러시가 되면서 쿼리가 날라가고 실제 데이터베이스 트랜잭션에 커밋된다.

`hibernate.jdbc.batch_size` : @@사이즈만큼 모아서 데이터베이스에 한 방에 네트워크로 이 쿼리 두 방을 딱 보내고 DB 커밋

#### - 엔티티 수정. 변경 감지(Dirty Checking)
```java
// 영속 엔티티 데이터 수정
member.setName("zzz"); //변경

//em.update(member); //할 필요 X. 자동으로 변경 감지됨. 업데이트 쿼리 자동으로 나감
```
<img width="515" height="282" alt="image" src="https://github.com/user-attachments/assets/f5c0f8ad-0581-41b3-b1fc-9d3e9e046cc4" />

JPA는 데이터베이스 트랜잭션을 커밋하는 시점에
1. 내부적으로 flush() 호출됨
2. 엔티티랑 스냅샷 비교(값을 읽어온 시점. 1차 캐시 들어온 최초 시점의 상태 스냅샷)
3. 비교했더니 변경된 게 있네? → 업데이트 쿼리를 쓰기 지연 SQL 저장소에 또 만들어둠

## 2. 플러시
> 영속성 컨텍스트의 변경내용을 데이터베이스에 반영하는 것

데이터베이스 트랜잭션이 커밋되면 플러시가 자동으로 발생함

### 2.1 플러시 발생
- 변경 감지
- 수정된 엔티티 쓰기 지연 SQL 저장소에 등록
- 쓰기 지연 SQL 저장소의 쿼리를 데이터베이스에 전송 (등록, 수정, 삭제 쿼리)

플러시가 발생한다고 해서 데이터베이스 트랜잭션이 커밋되는 건 아님..
그리고 플러시 한다고 해서 1차 캐시가 사라지는 건 아님.

### 2.2 영속성 컨텍스트를 플러시하는 방법
#### - em.flush(): 직접 호출
근데 직접 쓸일 거의 없음.

테스트할 때, 미리 데이터베이스에 반영 또는 미리 쿼리 보고싶을 때 플러시 강제 호출

#### - 트랜잭션 커밋: 플러시 자동 호출

#### - JPQL 쿼리 실행: 플러시 자동 호출
INSERT 쿼리가 아직 안날라갔는데 DB에서 조회하려고 할 때 문제 생길 수 있으니까, JPA는 이런 문제를 방지하고자 JPQL 쿼리를 실행할 때는 무조건 플러시를 날림
```java
em.persist(memberA);
em.persist(memberB);
em.persist(memberC);

// 중간에 JPQL 실행하면 플러시 자동 호출
query = em.createQuery("select m from Member m", Member.class);
List<Member> members = query.getResultList();
```

### 2.3 플러시 모드 옵션
`em.setFlushMode(FlushModeType.COMMIT)`

- **FlushModeType.AUTO** : 커밋이나 쿼리를 실행할 때 플러시(기본값)
- **FlushModeType.COMMIT** : 커밋할 때만 플러시(쿼리를 실행할 때는 플러시X)

위 예시에서 JPQL에서 Member 테이블이 아니라 전혀 관계없는 테이블 조회할 때는 굳이 플러시 할 필요없으니까 플러시모드를 COMMIT으로 해도 됨.

근데 큰 도움은 안되니까 그냥 AUTO 써라

#### 플러시는
- 영속성 컨텍스트를 비우지 않음
- 영속성 컨텍스트의 변경내용을 데이터베이스에 동기화
- 트랜잭션이라는 작업 단위가 중요 → 커밋 직전에만 동기화하면 됨

## 3. 준영속 상태
### 3.1 준영속 상태
- 영속 → 준영속
- 영속 상태의 엔티티가 영속성 컨텍스트에서 분리(detached)
- 영속성 컨텍스트가 제공하는 기능 사용 못함 (업데이트나 dirty checking 등)

### 3.2 준영속 상태로 만드는 방법
#### - em.detach(entity) - 특정 엔티티만 준영속 상태로 전환
이제 entity를 JPA가 관리 안하겠다. 그래서 수정하고 트랜잭션 커밋해도 아무일도 안 일어남

#### - em.clear() - 영속성 컨텍스트를 완전히 초기화
엔티티 매니저에 있는 영속성 컨텍스트를 통째로 다 지워버림. (1차 캐시도 초기화됨)

#### - em.close() - 영속성 컨텍스트를 종료

