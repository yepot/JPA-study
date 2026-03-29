# Chapter04. 엔티티 매핑

[ 엔티티 매핑 소개 ]

객체와 테이블 매핑: `@Entity`, `@Table`

필드와 컬럼 매핑: `@Column`

기본 키 매핑: `@Id`

연관관계 매핑: `@ManyToOne`, `@JoinColumn` (다음장)

## 1. 객체와 테이블 매핑

### 1.1 @Entity
> @Entity가 붙은 클래스는 JPA가 관리하는 엔티티이다.

즉, JPA를 사용해서 테이블과 매핑할 클래스는 @Entity 필수!

#### 주의 !!
- public 또는 protected 기본 생성자가 있어야 함
- final 클래스, enum, interface, inner 클래스 사용 X
- DB에 저장하고 싶은 필트에는 final 사용 X

#### @Entity 속성 정리
속성: name
- JPA에서 사용할 엔티티 이름을 지정
- 보통 클래스 이름 그대로 사용(기본값)
- 예) `@Entity(name = "Member")`

### 1.2 @Table
> @Table은 엔티티와 매핑할 테이블 지정

#### 속성 종류
- name: 매핑할 테이블 이름, 기본값: 엔티티 이름을 사용

  예) `@Table(name = "MBR")` 그러면 쿼리 날라갈 때 MBR로 날라감
  
- catalog: 데이터베이스 catalog 매핑
- schema: 데이터베이스 schema 매핑
- uniqueConstraints(DDL): DDL 생성 시에 유니크 제약 조건 생성

## 2. 데이터베이스 스키마 자동 생성
### 2.1 데이터베이스 스키마 자동 생성

- JPA에서는 애플리케이션 실행 시점에 DB 테이블 자동 생성(create문으로)
- 테이블 중심 -> 객체 중심
- 데이터베이스 방언을 활용해서 데이터베이스에 맞는 적절한 DDL 생성
- 생성된 DDL은 운영에서 사용 X(또는 다듬기). 개발할 때만 사용해라

### 2.2 데이터베이스 스키마 자동 생성 - 속성
#### hibernate.hbm2ddl.auto
| 옵션 | 설명 |
| :--- | :--- |
| create | 기존테이블 삭제 후 다시 생성 (DROP + CREATE) |
| create-drop | create와 같으나 종료시점에 테이블 DROP |
| update | 변경된 부분만 반영(운영DB에는 사용하면 안됨) |
| validate | 엔티티와 테이블이 정상 매핑되었는지만 확인 |
| none | 사용하지 않음 |

`<property name="hibernate.hbm2ddl.auto" value="**여기에 옵션 넣기**" />`

### 2.3 주의할 점
#### 운영 장비에서는 절대 create, create-drop, update 사용 X !!

- 개발 초기 단계: create 또는 update
- 테스트 서버: update 또는 validate
- 스테이징과 운영 서버: validate 또는 none

테스트나 개발 서버에서도 직접 스크립트 짜서 적용해보고 문제 없으면 운영 서버에 적용하는 걸 권장

### 2.4 DDL 생성 기능
DDL을 자동 생성할 때만 딱 도와준다! (실행할 때 런타임 등에 영향은 안준다)

### nullable, length
예) `@Column(nullable = false, length = 10)`

→ 필수, 10자 초과 X

### unique 제약조건
예) `@Table(uniqueConstraints = {@UniqueConstraint(name = "NAME_AGE_UNIQUE", columnNames = {"NAME", "AGE})})`

→ 이름(NAME)과 나이(AGE)가 동시에 같은 데이터는 딱 하나만 존재해야 한다

→ name = "NAME_AGE_UNIQUE" : 제약조건의 이름, columnNames = {"NAME", "AGE"}: 중복을 체크할 대상 컬럼들

## 3. 필드와 컬럼 매핑

예시 코드
```java
package hellojpa;
    
import javax.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Date;
    
@Entity
public class Member {

    @Id
    private Long id;
        
    @Column(name = "name")
    private String username;
        
    private Integer age;
        
    @Enumerated(EnumType.STRING)
    private RoleType roleType;
        
    @Temporal(TemporalType.TIMESTAMP) //과거 버전
    private Date createdDate;
        
    @Temporal(TemporalType.TIMESTAMP) //과거 버전
    private Date lastModifiedDate;

    private LocalDate testLocalDate; //최신 버전
    private LocalDateTime testLocalDateTime; //최신 버전
        
    @Lob
    private String description;
}
```

### 3.1 매핑 어노테이션 정리
#### hibernate.hbm2ddl.auto
| 어노테이션 | 설명 |
| :--- | :--- |
| @Column | 컬럼 매핑 |
| @Temporal | 날짜 타입 매핑 |
| @Enumerated | enum 타입 매핑 |
| @Lob | BLOB, CLOB 매핑 (VARCHAR를 넘어선 큰 컨텐츠를 넣고 싶을 때) |
| @Transient | 특정 필드를 컬럼에 매핑하지 않음(매핑 무시). 메모리에서만 쓰겠다. |

### @Column
| 속성 | 설명 | 기본값 |
| :--- | :--- | :--- |
| name | 필드와 매핑할 테이블의 컬럼 이름 | 객체의 필드 이름 |
| insertable, updatable | 등록, 변경 가능 여부 | TRUE |
| nullable(DDL) | null 값의 허용 여부 | |
| unique(DDL) | 한 컬럼에 대해 유니크 제약조건을 걸 때 사용 | 제약조건 이름 반영이 이상해서 @Table unique 제약조건 사용 추천 |
| columnDefinition(DDL) | 데이터베이스 컬럼 정보를 직접 줄 수 있다. ex) varchar(100) default 'EMPTY' | 필드의 자바 타입과 방언 정보를 사용 |
| length(DDL) | 문자 길이 제약조건, String 타입에서만 사용! | 255 |
| precision, scale(DDL) | BigDecimal 타입에서 사용한다(BigInteger도 사용할 수 있다). precision은 소수점을 포함한 전체 자릿수를, scale은 소수의 자릿수다. 참고로 double, float 타입에는 적용되지 않는다. 아주 큰 숫자나 정밀한 소수를 다루어야 할 때만 사용한다. | precision=19, scale=2 |

### @Enumerated
자바 enum 타입을 매핑할 때 사용

속성: value

- EnumType.ORDINAL: enum 순서를 데이터베이스에 저장
- EnumType.STRING: enum 이름을 데이터베이스에 저장 
 
기본값은 ORDINAL 인데 

*주의!! ORDINAL 사용 X !! enum이 늘어나면 순서가 바뀌어버림

### @Temporal
날짜 타입을 매핑 할 때 사용

근데 최근에는 하이버네이트가 지원해줘서 어노테이션 없어도 LocalDate, LocalDateTime 사용할 때는 생략 가능

속성: value

- TemporalType.DATE: 날짜, 데이터베이스 date 타입과 매핑 (예: 2026-03-29)
- TemporalType.TIME: 시간, 데이터베이스 time 타입과 매핑 (예: 11:11:11)
- TemporalType.TIMESTAMP: 날짜와 시간, 데이터베이스 timestamp 타입과 매핑 (예: 2026-03-29 11:11:11)

### @Lob
데이터베이스 BLOB, CLOB 타입과 매핑

@Lob에는 지정할 수 있는 속성이 없음

- CLOB: String, char[], java.sql.CLOB
- BLOB: byte[], java.sql.BLOB

### @Transient
필드 매핑 X, 데이터베이스에 저장 X 하고싶을 때

## 4. 기본 키 매핑
### 4.1 기본 키 매핑 어노테이션
```java
@Id
@GeneratedValue(strategy = GenerationType.AUTO)
private Long id;
```

- 직접 할당 : @Id 만 사용
- 자동 생성 : @GeneratedValue → 값을 자동으로 할당 해줌

  | 전략 (Strategy) | 설명 | 비고 |
  | :--- | :--- | :--- |
  | **IDENTITY** | 데이터베이스에 위임 | MYSQL 등에서 사용 |
  | **SEQUENCE** | 데이터베이스 시퀀스 오브젝트 사용 | ORACLE 등에서 사용, `@SequenceGenerator` 필요 |
  | **TABLE** | 키 생성용 테이블 사용 | 모든 DB에서 사용 가능, `@TableGenerator` 필요 |
  | **AUTO** | 방언에 따라 자동 지정 | 기본값 |

### 4.2 IDENTITY 전략
#### 특징
- 기본 키 생성을 데이터베이스에 위임.
- `em.persist()` 호출 시점에 즉시 INSERT 쿼리가 실행된다.
- 주로 MySQL, PostgreSQL, SQL Server, DB2에서 사용 (예: MySQL에서 auto_increment)
- auto_increment는 데이터베이스에 INSERT SQL을 실행한 이후에 ID값을 할 수 있음

#### 매핑
```java
@Entity
public class Member {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
```

### 4.3 SEQUENCE 전략
#### 특징
- 유일한 값을 순서대로 생성하는 특별한 데이터베이스 오브젝트
- 오라클, PostgreSQL, DB2, H2 데이터베이스에서 사용

시퀀즈 전략이네! DB에서 값을 얻어와서 멤버의 ID 값을 넣어준다. 그 다음에 영속성 컨텍스트에 딱 저장한다. 아직 DB에 쿼리는 안날라간다. 영속성 컨텍스트에 쌓여있고 실제 트랜잭션을 커밋하는 시점에 INSERT 쿼리가 날라간다.

IDENTITY는 INSERT 쿼리를 날려야 알 수 있으니까 이게 안됐음.

#### 매핑
```java
@Entity
@SequenceGenerator(
        name = “MEMBER_SEQ_GENERATOR",
        sequenceName = “MEMBER_SEQ", //매핑할 데이터베이스 시퀀스 이름
        initialValue = 1, allocationSize = 1)
public class Member {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE,
                  generator = "MEMBER_SEQ_GENERATOR")
    private Long id;
```
#### @SequenceGenerator
| 속성 | 설명 | 기본값 |
| :--- | :--- | :--- |
| name | 식별자 생성기 이름 | 필수 |
| sequenceName | 데이터베이스에 등록되어 있는 시퀀스 이름 | hibernate_sequence |
| initialValue | DDL 생성 시에만 사용됨, 시퀀스 DDL을 생성할 때 처음 1 시작하는 수를 지정한다. | 1 |
| allocationSize | 시퀀스 한 번 호출에 증가하는 수(성능 최적화에 사용됨) 데이터베이스 시퀀스 값이 하나씩 증가하도록 설정되어 있으면 이 값을 반드시 1로 설정해야 한다 | **50** |
| catalog, schema | 데이터베이스 catalog, schema 이름 | |

#### 최적화
- 미리 db에 50개를 미리 올려놓고 메모리에서 그 개수만큼 쓰는 방식

  - 처음 호출할 때 DB 시퀀스를 확 올려버림 (예: 1에서 바로 51로 업데이트)
  - 그리고 서버 메모리에 "나 지금 1번부터 50번까지는 내가 써도 된다고 허락받았어!"라고 저장해둠
  - 이후 50번의 persist()가 일어나는 동안은 DB에 가지 않고 메모리에서 번호를 하나씩 꺼내 씀!
- 여러 웹서버가 있어도 동시성 이슈 없이 다양한 문제들을 해결할 수 있음

예) allocationSize = 50 일 때
| 실행 코드 | DB SEQ 값 | 메모리(MEM) 사용 여부 | 설명 |
| :--- | :---: | :---: | :--- |
| `em.persist(member1);` | **1, 51** | 최초 호출 | DB에서 1~50번까지 사용 권한을 받아옴 (SEQ는 51이 됨) |
| `em.persist(member2);` | 51 | **MEM** | DB에 가지 않고 메모리에서 2번 꺼내 씀 |
| `em.persist(member3);` | 51 | **MEM** | DB에 가지 않고 메모리에서 3번 꺼내 씀 |

### 4.4 Table 전략
#### 특징
모든 db에 다 적용할 수 있다. 테이블 하나를 만들어서 거기서 키를 계속 생성하는 것

락도 걸릴 수가 있고..성능이 좀 떨어진다는 단점

#### 매핑
```sql
create table MY_SEQUENCES (
    sequence_name varchar(255) not null,
    next_val bigint,
    primary key ( sequence_name )
)
```

```java
@Entity
@SequenceGenerator(
        name = “MEMBER_SEQ_GENERATOR",
        table = “MY_SEQUENCES",
pkColumnValue = "MEMBER_SEQ", allocationSize = 1)
public class Member {

    @Id
    @GeneratedValue(strategy = GenerationType.TABLE,
                  generator = "MEMBER_SEQ_GENERATOR")
    private Long id;
```


#### @TableGenerator - 속성
| 속성 | 설명 | 기본값 |
| :--- | :--- | :--- |
| name | 식별자 생성기 이름 | 필수 |
| table | 키생성 테이블명 | hibernate_sequences |
| pkColumnName | 시퀀스 컬럼명 | sequence_name |
| valueColumnName | 시퀀스 값 컬럼명 | next_val |
| pkColumnValue | 키로 사용할 값 이름 | 엔티티 이름 |
| **initialValue** | 초기 값, 마지막으로 생성된 값이 기준이다. | 0 |
| **allocationSize** | 시퀀스 한 번 호출에 증가하는 수(성능 최적화에 사용됨) | **50** |
| catalog, schema | 데이터베이스 catalog, schema 이름 | |
| uniqueConstraints(DDL) | 유니크 제약 조건을 지정할 수 있다. | |

### 4.5 권장하는 식별자 전략
- 기본 키 제약 조건: not null, unique, 변하면 X
- 미래까지 이 조건을 만족하는 자연키(주민등록번호 등)를 찾기 어려움. 대체키를 사용하자

→ **Long형 + 대체키 + 키 생성전략 사용** 을 권장함!

즉, auto_increment나 sqeuence-object 둘 중 하나 사용해라

