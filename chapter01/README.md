# chapter01. JPA 소개

## 1. SQL 중심적인 개발의 문제점
### 1-1. SQL 중심적인 개발의 문제점
#### CRUD 반복 작업
객체 구조가 변경할 때마다 SQL을 함께 수정해야함 → SQL에 의존적인 개발을 피하기 어려움
#### 패터다임의 불일치: 객체 vs 관계형 데이터베이스
| 객체지향   | 관계형 데이터베이스 |
| ------ | ---------- |
| 객체     | 테이블        |
| 참조     | 외래키        |
| 상속     | 없음(슈퍼타입/서브타입)         |
| 객체 그래프 | JOIN       |

현실적인 대안은 관계형 데이터베이스를 써야하긴한다..

> 객체 → SQL 변환 → RDB 전달

결국 개발자는 SQL 매퍼 역할을 수행

### 1-2. 객체와 관계형 데이터베이스의 차이
#### 상속
관계형 데이터베이스는 상속관계 대신 슈퍼타입 서브타입 관계로 한다.

#### 연관관계
객체는 참조를 사용. 객체를 테이블에 맞추어 모델링 
테이블은 외래키를 사용

#### 객체 그래프 탐색
객체 모델링을 저장, 조회 할 때 번거로움
근데 객체 모델링을 자바 컬렉션에 관리할 때는 이미 멤버랑 팀에 연관관계를 걸어놨기 떄문에 한줄로만 멤버 관리 가능!

```java
list.add(member);

Member member = list.get(memberId);
Team team = member.getTeam();
```
- 객체는 자유롭게 객체 그래프를 탐색할 수 있어야 한다.
- 점점점으로 따라감.
- 탐색 범위는 처음 실행하는 SQL에 따라서 결정된다.

→ 엔티티 신뢰 문제 → 그래서 다음 계층의 코드도 까봐야하긴함.

근데 모든 객체를 미리 로딩할 수 없음. 상황에 따라 동일한 회원 조회 메서드를 여러벌 생성

예) Member.getMemberWithTeam 이라는 메서드명을 만든다. but, 이것도 한계 존재!

계층형 아키텍처는 어쩔 수 없이 진정한 의미의 계층 분할은 어렵다! (계층형 아키텍처의 문제)

#### 동일성 비교 문제
```java
String memberId = "100";

Member member1 = member.getMember(memberId);
Member member2 = member.getMember(memberId);

member1 == member2; // 다르다
```
```java
String memberI = "100";

Member member1 = list.get(memberId);
Member member2 = list.get(memberId);

member1 == member2; // 같다
```

컬렉션에서는 같은 인스턴스를 조회하게 되면 걔는 같은 참조로 나오게 된다. (같은 인스턴스)


이 외에도 데이터 타입, 데이터 식별 방법에도 차이가 존재함.

객체를 자바 컬렉션에 저장하듯이 DB에 저장하고싶다 → JPA

## 2. JPA 소개
### 2-1. JPA란?
> #### JPA
- Java Persistence API
- 자바 진영의 ORM 기술 표준

> #### ORM
객체와 관계형 데이터베이스를 연결해주는 기술이다.
- Object-relation mapping (객체 관계 매핑)
- 객체는 객체대로 설계
- 관계형 데이터베이스는 관계형 데이터베이스대로 설계
- ORM 프레임워크가 중간에서 매핑

JPA는 애플리케이션과 JDBC 사이에서 동작
```
Application
    ↓
   JPA
    ↓
  JDBC
    ↓
Database
```

JPA가 객체 분석해서 쿼리 다 만들고 그 다음에 JDBC API를 사용한다.

#### JPA는 표준 명세
- JPA는 구현체가 아니라 인터페이스 기반의 표준 명세
- 대표적인 JPA 구현체: Hibernate, EclipseLink, DataNucleus

실제 프로젝트에서는 Hibernate를 가장 많이 사용함

### 2-2. JPA를 왜 사용해야 하는가?
#### 생산성 (JPA와 CRUD)
- 저장: jpa.**persist**(member)
- 조회: Member member = jap.**find**(memberId)
- 수정: member.**setName**("변경할 이름")
- 삭제: jpa.**remove**(member)

#### 유지보수
- 기존: 필드 변경 시 모든 SQL 수정
- JPA: 필드만 추가하면 됨. SQL은 JPA가 처리

#### JPA와 패러다임의 불일치 해결
- JPA와 상속: JPA가 내부적으로 JOIN SQL을 생성하여 처리
- JPA와 연관관계: 객체의 참조 관계를 그대로 사용할 수 있음
- JPA와 객체 그래프 탐색: 객체의 참조를 통해 자연스럽게 탐색 가능

신뢰할 수 있는 엔티티와 계층. JPA를 사용하면 객체 그래프 탐색 가능, 엔티티 신뢰 가능, 계층 구조 유지 가능!

- JPA와 비교하기: 같은 트랜잭션에서 조회한 엔티티는 동일성을 보장함

#### JPA의 성능 최적화 기능
- 1차 캐시와 동일성 보장

  JPA는 1차 캐시(EntityManager 내부)를 사용한다.
  1. 첫 조회 → SQL 실행
  2. 동일 엔티티 재조회 → 1차 캐시에서 반환
  
- 트랜잭션을 지원하는 쓰기 지연

  - 트랜잭션을 커밋할 때까지 INSERT SQL을 모음.
  - JDBC의 Batch SQL이라는 기능을 사용해서 한번에 SQL 전송

- 지연 로딩(Lazy Loading)

  - 지연로딩: 객체가 실제 사용될 때 로딩
  - 즉시 로딩: JOIN SQL로 한 번에 연관된 객체까지 미리 조회

  일단 지연 로딩으로 쭉 해두고 나중에 이런 것들을 하나의 쿼리로 하는 게 좋을 것 같다 그러면 거기만 즉시로딩으로 바꿔주면 좋다!
