# 10. 객체지향 쿼리 언어(1)
## 1. 객체지향 쿼리 언어

- JPA는 객체(Entity) 중심으로 데이터베이스를 조회하는 기능을 제공한다.
- 이걸 가능하게 하는 대표적인 쿼리 언어가 JPQL
- JPA는 SQL을 직접 작성하지 않고, 객체 모델을 그대로 사용하는 쿼리 언어를 사용하도록 설계되어 있음.

#### JPA의 다양한 쿼리 방법
- JPQL: JPA의 표준 객체지향 쿼리 언어
- JPA Criteri: 동적 쿼리를 타입 안정성 있게 작성할 수 있는 기능
- QueryDSL: JPQL의 단점을 보완하고 동적 쿼리를 쉽게 작성하도록 도와주는 라이브러리
- 네이티브 SQL: 데이터베이스의 SQL을 그대로 사용할 때
- JDBC API, MyBatis, SpringJdbcTemplate: JPA 외에 JDBC를 직접 사용하거나, 다른 프레임워크와 함께 데이터베이스를 제어하는 방식

### 1.1 JPQL
#### JPQL (Java Persistence Query Language) 이란
> JPA의 객체 지향 쿼리 언어 (객체 지향 SQL이라고 생각..)
- 테이블(X) → 엔티티(Entity)를 대상으로 쿼리 작성
- SQL과 문법 유사: SELECT, FROM, WHERE, JOIN 등
- DB 독립적이고 결과는 엔티티 객체로 반환

#### JPQL의 주요 특징
- 모든 데이터를 객체로 변환해 검색은 불가 → 필요한 조건 포함해야 한다.
- 결국 SQL로 변환돼 실행된다.


#### JPQL vs. SQL

|구분|JPQL|SQL|
|:---|:---|:---|
|대상|엔티티 객체|DB 테이블|
|독립성|DB 독립적|DBMS 종속|
|문법|SELECT 등 유사|SQL 표준|
|결과|엔티티 객체|데이터 행(row)|

#### JPQL 사용 예시
Member 엔티티의 이름이 "hello"인 사람을 찾고 싶다면
```java
String jpql = "SELECT m FROM Member m WHERE m.name LIKE '%hello%'";
List<Member> result = em.createQuery(jpql, Member.class).getResultList();
```

#### JPQL과 실행된 SQL
JPQL은 엔티티 기준으로 쿼리를 작성한다.

```java
String jpql = "select m from Member m where m.age > 18";
List<Member> result = em.createQuery(jpql, Member.class)
                        .getResultList();
```

실행되는 SQL은 실제 DB의 테이블과 컬럼으로 변환된다.

```sql
select
    m.id as id,
    m.age as age,
    m.USERNAME as USERNAME,
    m.TEAM_ID as TEAM_ID
from
    Member m
where
    m.age > 18
```

->  JPQL은 객체(Entity)를 대상으로 쿼리 작성, 실행은 SQL로 변환 !


### 1.2 Criteria
> Criteria는 자바 코드로 JPQL을 작성할 수 있는 방식이다.

JPQL 빌더 역할을 하고, 타입 안정성을 보장하고 동적 쿼리에 유리하다.

#### 기본 사용 흐름

1. CriteriaBuilder 생성
```java
CriteriaBuilder cb = em.getCriteriaBuilder();
```

2. CriteriaQuery 객체 생성
```java
CriteriaQuery<Member> query = cb.createQuery(Member.class);
```

3. Root 객체 생성 (조회 시작할 엔티티)
```java
Root<Member> m = query.from(Member.class);
```

4. 조건절 포함 쿼리 작성
```java
CriteriaQuery<Member> cq = query.select(m).where(cb.equal(m.get("username"), "kim"));
List<Member> resultList = em.createQuery(cq).getResultList();
```

- 문자가 아닌 코드로 JPQL 작성 가능
- JPQL 빌더 역할 (타입 안정성)
- JPA 공식 기능

하지만, 너무 복잡하고 실용성이 부족하다. 대신 QuertDSL 사용을 권장함(더 쉽고 실무에서 많이 씀)

### 1.3 QueryDSL
> JPQL을 코드로 작성하는 라이브러리이다.

예시 코드

```java
JPAQueryFactory query = new JPAQueryFactory(em);
QMember m = QMember.member;

List<Member> list = query.selectFrom(m)
                         .where(m.age.gt(18))
                         .orderBy(m.name.desc())
                         .fetch();
```

JPQL로는 `select m from Member m where m.age > 18 order by m.name desc`와 동일한 쿼리

- 타입 안전: 컴파일 시점에 문법 오류를 잡아준다.
- 코드 기반: 문자열 X, 자바코드로 JPQL 작성한다.
- 동적 쿼리 작성 편리하다.
- 간단하고 실무에 적합하다.


### 1.4 네이티브 SQL
- JPA에서 JPQL로 해결할 수 없는 경우 직접 SQL을 사용할 수 있는 기능이다.
- 특정 DBMS에 의존하는 SQL (예: 오라클의 CONNECT BY)도 사용 가능하다.
- SQL 힌트 등 DB 독립성이 필요한 경우 제외하고 사용한다.

예시 코드

```java
String sql = "SELECT ID, AGE, TEAM_ID, NAME FROM MEMBER WHERE NAME = 'kim'";
List<Member> resultList = em.createNativeQuery(sql, Member.class).getResultList();
```

### 1.5 JDBC 직접 사용, SpringJdbcTemplate 등
- JPA를 사용하면서 JDBC 커넥션을 직접 사용하거나,스프링 JdbcTemplate, MyBatis 등과 함께 사용 가능하다.
- 영속성 컨텍스트를 적절한 시점에 강제로 플러시가 필요하다. (예: JPA 우회해 SQL을 실행하기 직전에 플러시)
- 직접 SQL을 실행하고 영속성 컨텍스트의 상태를 일관성 있게 유지해야 한다.

#### JPA의 다양한 쿼리 방법 비교

|방법|특징|장점|단점|주 사용처|
|:---|:---|:---|:---|:---|
| **JPQL**| JPA 표준 객체지향 쿼리 언어 (엔티티 대상)    | SQL 유사 문법, 객체 중심, DB 독립성 유지 | 동적 쿼리 불편, 복잡한 쿼리 작성 어려움 | 엔티티 중심 단순 조회           |
| **JPA Criteria**  | 코드 기반 JPQL 빌더 (타입 안정성)        | 컴파일 시점 오류 확인, 동적 쿼리 가능      | 복잡하고 가독성 낮음, 실무 비추천     | JPQL 빌더, 타입 안전 동적 쿼리   |
| **QueryDSL** | JPQL 단점 보완한 코드 기반 빌더          | 타입 안전, 동적 쿼리 편리, 가독성 좋음     | 별도 설정 필요(Q타입 생성), 러닝커브  | 실무 권장, 복잡한 동적 쿼리       |
| **네이티브 SQL**   | 데이터베이스 SQL 직접 사용              | JPQL로 처리 못하는 DB 특화 SQL 가능   | DB 종속성, 영속성 컨텍스트 관리 필요  | DB 특화 SQL, SQL 힌트      |
| **JDBC API, MyBatis, SpringJdbcTemplate** | JDBC 커넥션 직접 사용, MyBatis 연계 가능 | 세밀한 SQL 제어, JPA 외 데이터 제어 가능 | JPA의 영속성 컨텍스트 관리 필요     | SQL 직접 처리, 외부 프레임워크 연계 |


---

## 2. JPQL 기본 문법과 기능
JPQL은 객체(Entity)를 대상으로 SQL처럼 쿼리 작성한다. DB 독립적이고, 최종적으로는 SQL로 변환되어 실행한다.

<img width="509" height="500" alt="image" src="https://github.com/user-attachments/assets/89960f0c-e414-4ffd-8a5b-6c49d20ec64a" />

### 2.1 JPQL 문법
JPQL은 SQL과 비슷한 구조를 가지고 있지만, 대상은 엔티티(Entity)

```text
select_문 ::= select_절 from_절 [where_절] [groupby_절] [having_절] [orderby_절]
update_문 ::= update_절 [where_절]
delete_문 ::= delete_절 [where_절]
```

- 엔티티와 속성은 대소문자 구분 (Member, age)
- JPQL 키워드는 대소문자 구분 안함 (SELECT, FROM, where)
- 엔티티 이름 사용, 테이블 이름 아님 (Member)
- 별칭(m)은 필수 !! (as는 생략 가능)

### 2.2 집합과 정렬

#### 집합 함수 (SELECT 절)
```sql
select
    COUNT(m),       // 회원 수
    SUM(m.age),     // 나이 합
    AVG(m.age),     // 평균 나이
    MAX(m.age),     // 최대 나이
    MIN(m.age)      // 최소 나이
from Member m
```
- COUNT(): 엔티티 수
- SUM(): 합계
- AVG(): 평균
- MAX(), MIN(): 최대, 최소

#### 정렬 (GROUP BY, HAVING, ORDER BY)
- GROUP BY: 데이터를 그룹화
- HAVING: 그룹 조건 필터링
- ORDER BY: 정렬

### 2.3 TypeQuery, Query
- TypeQuery : 반환 타입이 명확할 때 사용 (예: Member 엔티티)
- Query : 반환 타입이 명확하지 않을 때 사용 (예: 특정 컬럼 값 등)

```java
// TypeQuery
TypedQuery<Member> query = em.createQuery("SELECT m FROM Member m", Member.class);

// Query
Query query = em.createQuery("SELECT m.username, m.age FROM Member m");
```

### 2.4 결과 조회 API
- query.getResultList() : 결과가 하나 이상일 때 리스트 반환

  결과 없으면 빈 리스트.
  ```java
  List<Member> members = query.getResultList();
  ```

- query.getSingleResult() : 결과가 정확히 하나일 때 단일 객체 반환. 주의 !!
  결과 없으면 NoResultException / 둘 이상이면 NonUniqueResultException 발생
  ```java
  Member member = query.getSingleResult();
  ```

### 2.5 파라미터 바인딩
JPQL 쿼리에서 조건값을 직접 하드코딩하지 않고 외부에서 바인딩하는 방법이다.

#### 이름 기준 바인딩
```java
SELECT m FROM Member m WHERE m.username = :username
query.setParameter("username", usernameParam);
```
- `:username` → 파라미터 이름 (콜론(:)과 함께 사용)
- `setParameter("username", usernameParam)` → 파라미터 이름에 실제 값 바인딩
- 장점: 가독성 좋고, 파라미터가 많아도 의미를 알기 쉬움

#### 위치 기준 바인딩 
```java
SELECT m FROM Member m WHERE m.username = ?1
query.setParameter(1, usernameParam);
```
- `?1` → 첫 번째 위치의 파라미터
- `setParameter(1, usernameParam)` → 해당 위치에 실제 값 바인딩
- 장점: 빠르고 간단, 파라미터 수가 적을 때 유용


위치 기준은 중간에 뭐 하나 끼워 넣어버리면 순서 다 밀리니까 웬만하면 쓰지말고 그냥 이름 기준 써라!!


---

## 3. 프로젝션 (SELECT)
> 프로젝션은 SELECT 절에 조회할 대상을 지정하는 것이다.

조회 대상에 따라 엔티티, 임베디드 타입, 스칼라 타입 등 다양한 결과를 가져올 수 있다.

### 3.1 프로젝션 종류
#### 엔티티 프로젝션
엔티티나 연관 엔티티 전체를 조회
```sql
SELECT m FROM Member m
SELECT m.team FROM Member m
```

#### 임베디드 타입 프로젝션
내장 타입(예: 주소)만 조회
```sql
SELECT m.address FROM Member m
```

(근데 엔티티 타입이 필요!!)

#### 스칼라 타입 프로젝션
단순 필드 값만 조회 (문자, 숫자 등)
```sql
SELECT m.username, m.age FROM Member m
```

#### 중복 제거
```sql
SELECT DISTINCT m FROM Member m
```

### 3.2 프로젝션 - 여러 값 조회
```sql
SELECT m.username, m.age FROM Member m
```
- **Query 타입** : 타입을 지정하지 않은 쿼리

- **Object[] 타입** : 결과를 배열로 조회

- **new 명령어(DTO)**: 단순 값을 DTO로 바로 조회
  - 패키지명 포함 전체 클래스명 입력
  - 생성자는 조회 대상 순서와 타입에 맞춰야 함
제일 깔끔한 방법이다:
```sql
SELECT new jpabook.jpql.UserDTO(m.username, m.age) FROM Member m
```

---
## 4. 페이징 API
JPA에서는 페이징 처리를 다음 두 메서드로 추상화해 제공한다.
페이징은 데이터의 부분 조회(페이지네이션) 처리를 위해 필요하다.

#### 사용 메서드
**`setFirstResult(int startPosition)`** → 조회 시작 위치 지정 (0부터 시작)

**`setMaxResults(int maxResult)`** → 조회할 데이터의 최대 개수 지정

예제 코드
```java
String jpql = "select m from Member m order by m.name desc";
List<Member> resultList = em.createQuery(jpql, Member.class)
                            .setFirstResult(10)   // 10번째부터 조회
                            .setMaxResults(20)    // 최대 20개 조회
                            .getResultList();
```

#### MySQL 방언
```sql
SELECT
    M.ID AS ID,
    M.AGE AS AGE,
    M.TEAM_ID AS TEAM_ID,
    M.NAME AS NAME
FROM
    MEMBER M
ORDER BY
    M.NAME DESC
LIMIT ?, ?
```
- `LIMIT ?, ?` : 페이징 처리를 위해 시작 위치와 최대 데이터 수 지정

#### Oracle 방언
```sql
SELECT * FROM
    ( SELECT ROW_.*, ROWNUM ROWNUM_
      FROM
        ( SELECT
              M.ID AS ID,
              M.AGE AS AGE,
              M.TEAM_ID AS TEAM_ID,
              M.NAME AS NAME
          FROM MEMBER M
          ORDER BY M.NAME
        ) ROW_
      WHERE ROWNUM <= ?
    )
WHERE ROWNUM_ > ?
```
- `ROWNUM` : Oracle에서 페이징 처리를 위해 행 번호를 생성
- 내부 서브쿼리에서 `ROWNUM <= ?`로 최대 행을 지정하고, 외부 쿼리에서 `ROWNUM_ > ?`로 시작 위치를 지정

---
## 5. 조인

### 5.1 내부 조인, 외부 조인, 세타 조인
#### 내부 조인(INNER JOIN)
```sql
SELECT m FROM Member m [INNER] JOIN m.team t
```

#### 외부 조인(LEFT OUTER JOIN)
```sql
SELECT m FROM Member m LEFT [OUTER] JOIN m.team t
```

#### 세타 조인(Theta Join): 관계가 없는 엔티티끼리 조인
```sql
SELECT COUNT(m) FROM Member m, Team t WHERE m.username = t.name
```

일대다는 FetchType.Lazy 주의!!

### 5.2 ON 절 사용
ON절을 활용한 조인(JPA 2.1부터 지원)

#### 1. 조인 대상 필터링
예 : 회원과 팀을 조인하면서, 팀 이름이 'A'인 팀만 조인
```java
// JPQL
SELECT m, t FROM Member m LEFT JOIN m.team t ON t.name = 'A'

// SQL
SELECT m.*, t.* FROM Member m LEFT JOIN Team t ON m.TEAM_ID = t.id AND t.name = 'A'
```

#### 2. 연관관계 없는 엔티티 외부 조인
예 : 회원의 이름과 팀의 이름이 같은 대상 외부 조인
```java
// JPQL
SELECT m, t FROM Member m LEFT JOIN Team t ON m.username = t.name

// SQL
SELECT m.*, t.* FROM Member m LEFT JOIN Team t ON m.username = t.name
```

---
## 6. 서브 쿼리
#### 기본 사용 예
나이가 평균보다 많은 회원
```sql
select m from Member m
where m.age > (select avg(m2.age) from Member m2)
```

한 건이라도 주문한 고객
```sql
select m from Member m
where (select count(o) from Order o where m = o.member) > 0
```

#### 서브 쿼리 지원 함수
- `[NOT] EXISTS (subquery)` : 서브쿼리에 결과가 존재하면(혹은 없으면) 참

- `ALL (subquery)` : 모든 조건 만족하면 참

- ` ANY, SOME (subquery)` : 조건 중 하나라도 만족하면 참

- `[NOT] IN (subquery)` : 서브쿼리 결과 중 하나라도 같은 값이 있으면 참

#### JPQL 서브쿼리 예제
1. 팀 소속인 회원 조회
```sql
select m from Member m
where exists (select t from m.team t where t.name = '팀A')
```

2. 전체 상품 각가의 재고보다 주문량이 많은 주문들
```sql
select m from Member m
where exists (select t from m.team t where t.name = '팀A')
```

3. 어떤 팀이든 팀에 소속된 회원
```sql
select m from Member m
where m.team = ANY (select t from Team t)
```

#### 하이버네이트6 변경 사항
하이버네이트6부터 FROM 절의 서브쿼리를 지원!

이전에는 JPQL은 WHERE, HAVING 절에서만 서브쿼리가 가능했지만, 하이버네이트6부터 FROM 절도 지원된다.

#### JPA 서브쿼리의 한계
- **WHERE, HAVING 절**에서만 서브쿼리 사용 가능
- **SELECT 절**은 하이버네이트 등 일부 구현체에서 지원
- **FROM 절 서브쿼리**는 현재 JPQL에서는 불가능 

→ 조인으로 풀 수 있으면 풀어서 해결

또는 (정 안되면 네이티브 SQL)

또는 쿼리 두 번 날리기

---

## 7. JPQL 타입 표현과 기타식
### 7.1 JPQL 타입 표현

| 항목          | 예시                                       | 설명                                          |
| ----------- | ---------------------------------------- | ------------------------------------------- |
| **문자**      | `'HELLO'`, `'She''s'`                    | 문자열은 작은따옴표(`'`)로 감싸며, 작은따옴표는 이스케이프(`''`) 처리 |
| **숫자**      | `10L(Long)`, `10D(Double)`, `10F(Float)` | 숫자 값 표현, 접미사(L/D/F)로 타입 지정                  |
| **Boolean** | `TRUE`, `FALSE`                          | 불리언 값 표현                                    |
| **ENUM**    | `jpabook.MemberType.Admin`               | 패키지명을 포함한 Enum 상수 값 지정                      |
| **엔티티 타입**  | `TYPE(m) = Member`                       | 상속 관계에서 특정 엔티티 타입만 조회할 때 사용                 |

### 7.2 JPQL 기타
| 항목               | 내용                              | 설명                                    |
| ---------------- | ------------------------------- | ------------------------------------- |
| **SQL과 유사한 문법**  | JPQL은 SQL과 비슷한 문법 지원            | 기존 SQL과 유사한 구조로 작성 가능                 |
| **EXISTS, IN**   | 존재 여부 및 포함 여부 확인                | `EXISTS`: 하위 쿼리 존재 여부, `IN`: 목록 포함 여부 |
| **AND, OR, NOT** | 논리 연산자                          | 조건을 조합할 때 사용                          |
| **비교 연산자**       | `=`, `>`, `>=`, `<`, `<=`, `<>` | 값 비교를 위한 연산자                          |
| **기타 연산자**       | `BETWEEN`, `LIKE`, `IS NULL`    | 범위, 패턴, null 여부 확인 등 조건 처리            |

---

## 8. 조건식
### 8.1 CASE 식
조건에 따라 반환값을 다르게 지정한다.
#### 기본 CASE 식 (조건 여러 개)
```sql
select 
  case when m.age <= 10 then '학생요금' 
       when m.age >= 60 then '경로요금' 
       else '일반요금' 
  end 
from Member m
```
#### 단순 CASE 식 (하나의 컬럼 값 비교)
```sql
select 
  case t.name 
    when '팀A' then '인센티브110%' 
    when '팀B' then '인센티브120%' 
    else '인센티브105%' 
  end 
from Team t
```

### 8.2 COALESCE
여러 값 중 null이 아닌 첫 번째 값 반환

예 : 이름이 없으면 기본값으로 대체
```sql
select coalesce(m.username, '이름 없는 회원') from Member m
```

### 8.3 NULLIF
두 값이 같으면 null 반환, 다르면 첫 번째 값 반환

예: 이름이 '관리자'면 null 반환, 아니면 본인 이름 반환
```sql
select nullif(m.username, '관리자') from Member m
```
---
## 9. JPQL 기본 함수
#### 문자열 처리
CONCAT, SUBSTRING, TRIM, LOWER, UPPER, LENGTH, LOCATE

#### 수학 함수
ABS, SQRT, MOD

#### 기타 함수
SIZE (컬렉션 크기), INDEX (JPA 용도)

#### 사용자 정의 함수 호출
하이버네이트 등 JPA 구현체는 사용 전 DB 방언에 사용자 정의 함수 등록 필요하다.

사용자 정의 함수 호출 예
```sql
select function('group_concat', i.name) from Item i
```

사용하려는 DB 방언에 해당 함수를 상속받아 등록하고 사용해야 한다.
