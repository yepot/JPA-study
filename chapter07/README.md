# Chapter07. 고급 매핑
## 1. 상속관계 매핑
#### 상속관계 매핑이란?
객체에서는 상속이 가능하지만, DB는 상속 개념이 없음. 그래서 JPA에서는 상속 구조를 DB에 맞게 변환해서 저장해야 함

이를 슈퍼타입-서브타입 매핑 이라고 한다.

#### 매핑 전략 3가지

|전략|설명|특징|
|:---:|:---:|:---:|
|JOINED 전략|슈퍼타입/서브타입 각각 테이블로 분리하여 조인|정규화, 저장공간 효율적, 조회시 JOIN 성능 저하
|SINGLE_TABLE 전략|모든 속성을 하나의 테이블에 저장|조회 빠름, 컬럼 NULL 많아짐, 테이블 커질 수 있음
|TABLE_PER_CLASS 전략|서브타입마다 테이블 생성|설계자/전문가 비추천, 쿼리 복잡, UNION 필요

### 1.1 주요 어노테이션

#### `@Inheritance(strategy = InheritanceType.XXX)`

상속 전략 지정용 어노테이션

strategy에는 아래 중 하나를 사용:

- `JOINED` : 조인 전략 (부모-자식 테이블 나눠서 JOIN)
- `SINGLE_TABLE` : 단일 테이블 전략 (한 테이블에 모두 저장)
- `TABLE_PER_CLASS` : 구현 클래스마다 테이블 (자식 테이블만 존재)

#### `@DiscriminatorColumn(name = "DTYPE")`

단일 테이블 전략 또는 조인 전략에서 어떤 자식 타입인지 구분하기 위해 사용하는 구분자 컬럼

예) DTYPE 컬럼에 Book, Album 같은 값 저장됨

#### `@DiscriminatorValue("XXX")`

해당 엔티티가 어떤 구분자 값을 가질지 지정

예: @DiscriminatorValue("Book") → DTYPE = 'Book'으로 저장

### 1.2 조인 전략
<img width="882" height="263" alt="image" src="https://github.com/user-attachments/assets/8e1bf176-cadd-47d8-bdd3-d424d3689114" />

예시 코드
```java
@Entity
@Inheritance(strategy = InheritanceType.JOINED)
@DiscriminatorColumn(name = "DTYPE")
public abstract class Item {
    @Id @GeneratedValue
    private Long id;
    private String name;
    private int price;
}

@Entity
@DiscriminatorValue("Album")
public class Album extends Item {
    private String artist;
}

@Entity
@DiscriminatorValue("Book")
public class Book extends Item {
    private String author;
    private String isbn;
}
```
Item, Album, Book 테이블이 따로 생성되고 JOIN으로 연결됨

#### 장점
- 테이블 정규화
- 외래 키 참조 무결성 제약조건 활용 가능
- 저장공간 효율화

#### 단점
- 조회 시 조인 많이 사용 → 성능 저하
- 조회 쿼리 복잡
- 데이터 저장 시 INSERT SQL 2번 호출 필요

→ 정규화가 잘 되지만, 성능이 민감한 시스템에서는 조회 속도 문제 고려 필요!

### 1.3 단일 테이블 전략
<img width="875" height="271" alt="image" src="https://github.com/user-attachments/assets/a757350e-260f-4c55-b9a4-2803d736fc2e" />


예시 코드
```java
@Entity
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
@DiscriminatorColumn(name = "DTYPE")
public abstract class Item {
    @Id @GeneratedValue
    private Long id;
    private String name;
    private int price;
}

@Entity
@DiscriminatorValue("Movie")
public class Movie extends Item {
    private String director;
    private String actor;
}
```
Item이라는 하나의 테이블에 모든 자식 클래스의 필드까지 포함

→ DTYPE 컬럼을 통해 Movie, Album 등 구분

#### 장점
- 조인 없이 빠른 조회 가능
- 쿼리 구조가 단순함
- 
#### 단점
- 자식 엔티티의 모든 컬럼이 null 허용
- 모든 데이터를 한 테이블에 저장 → 테이블 비대화 → 성능 저하 가능성

→ 조회는 빠르지만, 테이블이 커지고 null이 많아질 수 있음에 주의해야 함!

### 1.4 구현 클래스마다 테이블 전략
<img width="881" height="227" alt="image" src="https://github.com/user-attachments/assets/25361bf6-a9df-4f9e-8e9c-ea59cdec31f5" />

예시 코드
```java
@Entity
@Inheritance(strategy = InheritanceType.TABLE_PER_CLASS)
public abstract class Item {
    @Id @GeneratedValue
    private Long id;
    private String name;
    private int price;
}

@Entity
public class Book extends Item {
    private String author;
    private String isbn;
}
```

비추천 전략..DB 설계자와 ORM 전문가 모두가 잘 사용하지 않고, 유지보수와 성능 측면에서 불리

#### 장점
- 서브타입을 명확히 구분해서 처리할 때 효과적
- NOT NULL 제약조건 사용 가능

#### 단점
- 여러 자식 테이블을 함께 조회 시 성능 저하 (UNION 필요)
- 자식 테이블을 통합 조회하기 어려움
- ORM 전문가들도 실무에서 비추천

→ 분리성은 높지만, 통합 조회가 어렵고 성능도 낮아 잘 사용하지 않음!

---

## 2. @MappedSuperclass
- 공통 매핑 정보를 모아두는 용도의 부모 클래스에 사용
- 엔티티가 아님 → DB 테이블과 매핑되지 않음
- 자식 클래스가 상속받아 사용 

#### 예시 설명
<img width="878" height="423" alt="image" src="https://github.com/user-attachments/assets/0b46a3b2-38a9-4b4d-b9f0-8169410ae6e1" />

- 객체 모델
  - `Member`와 `Seller` 클래스에 `id`, `name` 필드가 공통됨
  - 이를 `BaseEntity`라는 부모 클래스로 분리 → `@MappedSuperclass` 적용

- DB 테이블
  - 실제 DB에는 `BaseEntity` 테이블이 존재하지 않음
  - `MEMBER`, `SELLER` 각각 독립 테이블로 존재하며 `id`, `name` 필드를 포함
  
1. 공통 매핑 클래스: BaseEntity
```java
import jakarta.persistence.MappedSuperclass;
import java.time.LocalDateTime;

@MappedSuperclass
public abstract class BaseEntity {

    private Long id;
    private String name;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // Getter, Setter (또는 Lombok 사용 가능)
}
```
2. 실제 엔티티 클래스들 : Member, Seller
Member.java
```java
import jakarta.persistence.Entity;
import jakarta.persistence.Id;

@Entity
public class Member extends BaseEntity {

    @Id
    private Long memberId;

    private String email;

    // Getter, Setter
}
```
Seller.java
```java
import jakarta.persistence.Entity;
import jakarta.persistence.Id;

@Entity
public class Seller extends BaseEntity {

    @Id
    private Long sellerId;

    private String shopName;

    // Getter, Setter
}
```

#### 주요 특징

|항목|설명|
|:---|:---|
|관계|상속관계 매핑 X
|매핑 대상|테이블과 매핑되지 않음|
|사용 목적|공통 필드를 자식 엔티티에 매핑|
|조회 불가|em.find(BaseEntity) 불가능|
|설계 형태|직접 사용하지 않음->추상 클래스 권장|

#### 주 용도
- 전체 엔티티에 공통 적용되는 등록일, 수정일, 등록자 등 필드를 재사용할 때
- 코드 중복을 줄이기 위한 매핑 전용 상속 구조에 적합

#### 참고 사항
@Entity 클래스는 엔티티 클래스나 @MappedSuperclass 클래스만 상속 가능
