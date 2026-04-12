# Chapter06. 다양한 연관관계 매핑
[ 연관관계 매핑 시 고려해야할 3가지 ]

**1. 다중성**: 엔티티 간 관계의 수

|관계|설명|어노테이션|
|:---|:---|:---|
다대일 (N:1)|여러 엔티티가 하나의 엔티티를 참조|@ManyToOne|
|일대다 (1:N)|하나의 엔티티가 여러 엔티티를 참조|@OneToMany|
|일대일 (1:1)|하나의 엔티티가 다른 하나의 엔티티와만 연결|@OneToOne|
|다대다 (N:M)|여러 엔티티가 서로 여러 엔티티와 연결|@ManyToMany|

**2. 단방향, 양방향**: 테이블은 외래 키 하나로 양방향 조인이 가능하지만, 객체는 참조 방향이 명확하다.

한쪽만 참조 → 단방향 / 양쪽 참조 → 양방향

*주의 : 양방향일 경우 연관관계의 주인 설정 필수!

**3. 연관관계의 주인**: 외래 키가 있는 객체(테이블)가 연관관계의 주인

주인은 insert / update 쿼리를 발생시킬 수 있고, 반대편은 단순 조회(read-only) 만 가능하다.

mappedBy란? 연관관계의 주인이 아님을 명시하는 어노테이션. 해당 필드가 읽기 전용이며, 실제 외래 키는 상대 객체에 있다.
## 1. 다대일 [N:1]
### 1.1 다대일 단방향
<img width="415" height="237" alt="image" src="https://github.com/user-attachments/assets/5c3e0a16-5a8f-4fef-8275-6c7bef565f76" />

- Member → Team만 참조한다. (Team은 Member를 모름)
- @ManyToOne 관계만 존재
- 테이블에서는 MEMBER 테이블이 TEAM_ID 외래 키를 가진다.

```java
@Entity
public class Member {
    @Id @GeneratedValue
    private Long id;

    private String username;

    @ManyToOne
    @JoinColumn(name = "TEAM_ID") // 외래 키 컬럼
    private Team team;
}
```

## 1.2 다대일 양방향
<img width="420" height="229" alt="image" src="https://github.com/user-attachments/assets/ab7d1646-8553-4595-944f-194e093db40f" />

- Member → Team을 참조하고,
- Team도 → List<Member>로 Member 목록을 참조
- 외래키가 있는 쪽이 연관관계의 주인
- mappedBy는 외래 키가 아닌 쪽에서만 사용!!

양방향 매핑을 사용하면 객체 그래프 탐색이 유리하다

```java
@Entity
public class Team {
    @Id @GeneratedValue
    private Long id;

    private String name;

    @OneToMany(mappedBy = "team") // 주인이 아님
    private List<Member> members = new ArrayList<>();
}

@Entity
public class Member {
    @Id @GeneratedValue
    private Long id;

    private String username;

    @ManyToOne
    @JoinColumn(name = "TEAM_ID") // 주인
    private Team team;
}
```

**연관관계 편의 메서드 (추천!)**

양방향일 때, 한쪽만 설정하면 일관성이 깨질 수 있으므로 편의 메서드 작성이 좋다

```java
public void setTeam(Team team) {
    this.team = team;
    team.getMembers().add(this);
}
```

## 2. 일대다 [1:N]
### 2.1 일대다 단방향
<img width="424" height="230" alt="image" src="https://github.com/user-attachments/assets/be778ac6-e46c-41d5-acfc-f69934540fac" />

- Team → List<Member>를 참조하지만, Member → Team을 참조하지 않는다. (단방향)
- 객체 관점: Team → Member / 테이블 관점: 외래 키는 MEMBER.TEAM_ID에 있다. → 참조는 일방향이지만 외래 키는 반대쪽에 있다.

```java
@Entity
public class Team {
    @Id @GeneratedValue
    private Long id;

    private String name;

    @OneToMany
    @JoinColumn(name = "TEAM_ID") // 꼭 명시해야 함!
    private List<Member> members = new ArrayList<>();
}
```

@JoinColumn을 생략하면 중간 조인 테이블을 사용하는 @OneToMany-@ManyToOne 조합으로 처리된다.

**단점**

외래 키가 있는 쪽(Member)은 연관관계를 모른다. Team 엔티티가 관리하지만 외래 키가 실제로는 다른 테이블에 있어 추가적인 UPDATE SQL이 발생한다. 그래서 실무에서는 거의 사용하지 않는다.

다대일 양방향을 사용하자!!

### 2.2 일대다 양방향
<img width="515" height="244" alt="image" src="https://github.com/user-attachments/assets/53f2c5b0-d97f-4f27-a48f-84b1c9273a32" />

- Team → members 참조 / Member → team 참조 → 마치 양방향처럼 사용하는 구조지만 공식 매핑은 아니다.

```java
@Entity
public class Team {
    @Id @GeneratedValue
    private Long id;

    private String name;

    @OneToMany
    @JoinColumn(name = "TEAM_ID")
    private List<Member> members;
}

@Entity
public class Member {
    @Id @GeneratedValue
    private Long id;

    private String username;

    @ManyToOne
    @JoinColumn(name = "TEAM_ID", insertable = false, updatable = false)
    private Team team;
}
```

Member 쪽 필드를 읽기 전용 필드로 설정! 이렇게 하면 Member.team은 조회만 가능. 외래 키는 여전히 Team이 관리

**단점**

JPA에서 공식 양방향 구조가 아니고, 관리와 조회가 분리되어 복잡하다.

차라리 다대일 양방향 매핑을 사용할 것을 추천!


## 3. 일대일 [1:1]
- 양쪽 모두 1:1 관계이므로 외래 키를 어느 테이블에 둘 것인지 선택 가능
- 외래 키에는 UNIQUE 제약조건을 걸어 1:1 관계를 보장
- 외래 키를 두는 위치에 따라 구조와 성능이 달라진다.

### 3.1 일대일: 주 테이블에 외래키 단방향
가장 일반적인 일대일 매핑 방식

<img width="442" height="226" alt="image" src="https://github.com/user-attachments/assets/bc4d12a8-6de9-4b37-8c15-1656a8149001" />

```java
@Entity
public class Member {
    @Id @GeneratedValue
    private Long id;

    private String username;

    @OneToOne
    @JoinColumn(name = "LOCKER_ID", unique = true)
    private Locker locker;
}
```

- @OneToOne은 내부적으로 @ManyToOne과 유사하게 작동
- 외래 키에 unique = true 제약 조건 필수

### 3.2 일대일: 주 테이블에 외래키 양방향
<img width="439" height="216" alt="image" src="https://github.com/user-attachments/assets/04904d80-58cf-4f90-bad0-67936a90dab7" />

```java
@Entity
public class Locker {
    @Id @GeneratedValue
    private Long id;

    private String name;

    @OneToOne(mappedBy = "locker")
    private Member member;
}
```
mappedBy는 외래 키가 있는 쪽이 주인임을 의미!

### 3.3 일대일: 대상 테이블에 외래키 단방향
<img width="439" height="222" alt="image" src="https://github.com/user-attachments/assets/ccc4a460-614f-4ad2-a0d1-082e2db0ce26" />

- Locker가 Member를 참조하고 외래 키를 가진다.(LOCKER.MEMBER_ID)
- JPA에서는 단방향 매핑을 지원하지 않는다.

→ 양방향으로만 구성해야 한다.

### 3.4 일대일: 대상 테이블에 외래키 양방향
<img width="450" height="236" alt="image" src="https://github.com/user-attachments/assets/0ed98573-71e1-4e36-8947-ed83efc8b06a" />

```java
@Entity
public class Locker {
    @Id @GeneratedValue
    private Long id;

    private String name;

    @OneToOne
    @JoinColumn(name = "MEMBER_ID", unique = true)
    private Member member;
}

@Entity
public class Member {
    @Id @GeneratedValue
    private Long id;

    private String username;

    @OneToOne(mappedBy = "member")
    private Locker locker;
}
```
- 대부분의 경우 주 테이블에 외래 키를 두고 단방향 혹은 양방향 매핑을 사용
- 외래 키를 대상 테이블에 두는 경우는 DB 구조 유지를 위한 특별한 이유가 있을 때만 선택

| 구분 | 주 테이블에 외래 키 | 대상 테이블에 외래 키 |
| :--- | :--- | :--- |
| **개념** | 주 객체가 대상 객체의 참조를 가지는 것처럼 주 테이블에 외래 키를 둠 | 대상 테이블에 외래 키가 존재함 |
| **선호도** | 객체지향 개발자 선호 (JPA 매핑 편리) | 전통적인 데이터베이스 개발자 선호 |
| **장점** | 주 테이블만 조회해도 대상 테이블에 데이터가 있는지 확인 가능 | 일대일에서 일대다 관계로 변경 시 테이블 구조 유지 가능 |
| **단점** | 값이 없으면 외래 키에 null 허용 필요 | 프록시 한계로 인해 지연 로딩 설정 시에도 항상 즉시 로딩됨 |

## 4. 다대다 [N:N]
<img width="518" height="192" alt="image" src="https://github.com/user-attachments/assets/fab3961a-e345-4979-bf29-b2e8275ef401" />

정규화된 테이블 2개만으로는 다대다 관계를 표현할 수 없다. 중간 연결 테이블(Member_Product 등) 을 사용하여 다대일-일대다 관계로 풀어야 함!

### 4.1 객체 모델에서는 다대다 가능
JPA는 @ManyToMany와 @JoinTable을 통해 객체 간 다대다 매핑 지원

<img width="540" height="206" alt="image" src="https://github.com/user-attachments/assets/793f181a-f367-4341-8b07-c267ea24a2fe" />

```java
@Entity
public class Member {
    @Id @GeneratedValue
    private Long id;

    private String username;

    @ManyToMany
    @JoinTable(name = "member_product",
        joinColumns = @JoinColumn(name = "member_id"),
        inverseJoinColumns = @JoinColumn(name = "product_id"))
    private List<Product> products = new ArrayList<>();
}
```

연결 테이블은 단순히 ID만 매핑하지 않고, 주문시간, 수량, 상태 등 부가 정보가 들어갈 수 있다.
따라서 단순 @ManyToMany는 실무에서 거의 사용 X

### 4.2 한계 극복 방법 : 연결 테이블을 엔티티로 승격
중간 테이블을 별도의 엔티티(Order, MemberProduct 등)로 만들고 **다대다 → 일대다 + 다대일**로 풀어낸다.

<img width="519" height="212" alt="image" src="https://github.com/user-attachments/assets/3e600bd0-fd0c-450a-a000-2c848f267f85" />

```java
@Entity
public class MemberProduct {
    @Id @GeneratedValue
    private Long id;

    @ManyToOne
    private Member member;

    @ManyToOne
    private Product product;

    private int orderAmount;
    private LocalDateTime orderDate;
}
```

실무에서는 대부분 다대다 관계를 직접 매핑하지 않고, 연결 엔티티를 만들어 OneToMany + ManyToOne 조합으로 설계함!

## 5. 주요 속성
### @JoinColumn
외래키 매핑 속성
| 속성 | 설명 | 기본값 |
| :--- | :--- | :--- |
| **name** | 매핑할 외래 키 이름 | 필드명 + _ + 참조하는 테이블의 기본 키 컬럼명 |
| **referencedColumnName** | 외래 키가 참조하는 대상 테이블의 컬럼명 | 참조하는 테이블의 기본 키 컬럼명 |
| **foreignKey(DDL)** | 외래 키 제약조건을 직접 지정할 수 있다. (테이블 생성 시에만 사용) | |
| **unique, nullable, insertable, updatable, columnDefinition, table** | @Column의 속성과 같다. | |

### @ManyToOne
다대일 관계 매핑
| 속성 | 설명 | 기본값 |
| :--- | :--- | :--- |
| **optional** | false로 설정하면 연관된 엔티티가 항상 있어야 한다. | TRUE |
| **fetch** | 글로벌 페치 전략을 설정한다. | @ManyToOne = FetchType.EAGER |
| **cascade** | 영속성 전이 기능을 사용한다. | |
| **targetEntity** | 연관된 엔티티의 타입 정보를 설정한다. (거의 사용 안 함) | |

### @OneToMany
일대다 관계 매핑
| 속성 | 설명 | 기본값 |
| :--- | :--- | :--- |
| **mappedBy** | 연관관계의 주인 필드를 선택한다. | |
| **fetch** | 글로벌 페치 전략을 설정한다. | @OneToMany = FetchType.LAZY |
| **cascade** | 영속성 전이 기능을 사용한다. | |
| **targetEntity** | 연관된 엔티티의 타입 정보를 설정한다. (거의 사용 안 함) | |
