# Chapter09. 값 타입
## 1. 기본값 타입
### 1.1 JPA의 데이터 타입 분류
#### 엔티티 타입
- `@Entity`로 정의된 객체
- 식별자 있음 → 변경되어도 추적 가능
- 예) 회원 엔티티에서 키, 나이 등의 속성이 변해도 동일 엔티티로 인식됨

#### 값 타입
- `int`, `Integer`, `String` 등 자바의 기본 타입이나 객체
- 식별자 없음 → 변경 시 추적 불가, 완전히 다른 값으로 인식

### 1.2 값 타입의 세부 분류
#### 기본값 타입
- 자바 기본 타입: `int`, `double`
- 래퍼 클래스: `Integer`, `Long`
- 문자열: `String`

#### 임베디드 타입
- 복합 값 타입 (예: 주소, 기간 등)
- `@Embeddable` 사용

#### 컬렉션 값 타입
- 값 타입을 리스트, 셋 등의 컬렉션 형태로 사용
- 예) `List<Address>` 등

### 1.3 기본값 타입의 특징
예: `String name`, `int age`

- 생명주기 = 소속 엔티티의 생명주기를 따름
  - 예) 회원 엔티티 삭제 시, 이름과 나이 필드도 함께 삭제됨

- 공유하면 안 됨
  - 값 타입은 참조 공유하면 예기치 않은 변경 발생 가능
  - 예) A회원의 이름을 변경했더니 B회원의 이름도 바뀌는 문제 발생

> 참고 : 자바의 기본 타입은 절대 공유 X

- 기본 타입(`int`, `double`) : 절대 공유 불가. 항상 값을 복사해서 전달됨
- 래퍼 클래스(`Integer`) : 객체지만 불변. 공유는 가능하나 값 변경은 불가
- `String`: 공유 가능한 객치이나 내부적으로 불변 객체. 변경 시 새로운 객체가 생성됨.

---
## 2. 임베디드 타입 (복합 값 타입)
### 2.1 임베디드 타입 (Embedded Type)
새로운 값 타입을 직접 정의 (예: 주소, 좌표 등 복합 개념을 하나의 타입으로 묶어서 표현할 수 있음)

기본 값 타입들을 묶어 만든 복합 값 타입

예) `int`, `String`과 같은 단순 값 타입을 여러 개 조합하여 정의

#### 예시 상황
회원 엔티티는 이름, 근무 시작일, 근무 종료일, 주소 도시, 주소 번지, 주소 우편번호를 가진다.

<img width="132" height="177" alt="image" src="https://github.com/user-attachments/assets/954727d9-1930-449a-b9bf-98047b8de58e" />

이걸 하나의 타입으로 묶어서 표현하면
→ 회원 엔티티는 이름, 근무 기간, 집 주소를 가진다.

<img width="350" height="182" alt="image" src="https://github.com/user-attachments/assets/5253b288-c2c0-44c3-9f82-a00e29c3f789" />
<img width="371" height="207" alt="image" src="https://github.com/user-attachments/assets/c16038e9-1282-4022-8ec5-32ad6f1e82ce" />

- `workPeriod` → `Period` 클래스 : startDate, endDate 포함
- `homeAddress` → `Address` 클래스 : city, street, zipcode 포함

#### 클래스 구조
`Period`
```java
@Embeddable
public class Period {
    private LocalDate startDate;
    private LocalDate endDate;
    
   	// 생성자, Getter, Setter...
}
```
`Address`
```java
@Embeddable
public class Address {
    private String city;
    private String street;
    private String zipcode;
    
    // 생성자, Getter, Setter...
}
```
`Member`
```java
@Entity
public class Member{

    @Id
    @GeneratedValue
    @Column(name="MEMBER_ID")
    private Long id;

    @Column(name="USERNAME")
    private String username;

    // 기간 Period
    @Embedded
    private Period workPeriod;

    // 주소 Address
    @Embedded
    private Address homeAddress;
    
    // 생성자, Getter, Setter...
}
```
@Embeddable, @Embedded 둘 중 하나만 작성해도 좋으나, 둘 다 쓰는 것을 권장



### 2.2 임베디드 타입 사용법
- `@Embeddable` : 값을 정의하는 클래스에 선언 (값 타입 정의 위치에 사용)
- `@Embedded` : 값을 사용하는 클래스(예: 엔티티)에 선언 (값 타입 사용하는 필드에 사용)
- 생성자 조건 : 기본 생성자 필수 (JPA는 리플렉션을 통해 객체 생성하므로)

### 2.3 임베디드 타입의 장점
- 재사용성: 공통 값 타입(주소, 기간 등)을 여러 엔티티에서 재사용 가능

- 높은 응집도 : 관련된 값들을 하나의 클래스(값 타입)으로 묶어 설계 가능

- 의미 있는 메소드 정의 가능 (예: Period.isWork() 같은 도메인 의미를 가진 메소드 정의)

- 생명주기 의존: 임베디드 타입은 그것을 포함한 엔티티의 생명주기에 의존함
  → 임베디드 타입은 독립적으로 저장되지 않음

### 2.4 임베디드 타입과 테이블 매핑

<img width="357" height="275" alt="image" src="https://github.com/user-attachments/assets/5682a668-0d9f-4d06-8b71-0dcb0c9c7f3c" />

#### 정의
임베디드 타입은 엔티티의 값일 뿐이며, 별도의 테이블이 생성되지 않음. 임베디드 타입을 쓰기 전과 후에 매핑되는 테이블은 동일

#### 테이블 매핑 방식
엔티티의 테이블에 임베디드 타입의 필드가 컬럼으로 평평하게(flat하게) 펼쳐짐

예)

Period의 startDate, endDate → STARTDATE, ENDDATE 컬럼

Address의 city, street, zipcode → CITY, STREET, ZIPCODE 컬럼

#### 특징
- 객체와 테이블 간에 매우 세밀한(find-grained) 매핑 가능
- 잘 설계된 ORM 애플리케이션일수록
  - 매핑하는 테이블 수 < 클래스 수
  - 한 테이블이 여러 클래스를 포함하는 구조를 가짐

### 2.5 임베디드 타입과 연관관계

<img width="460" height="142" alt="image" src="https://github.com/user-attachments/assets/4aeca516-52e4-4fee-9b8b-206e589a41fb" />

#### Member 엔티티
엔티티로서 `Address`와 `PhoneNumber`라는 임베디드 값 타입(Value Object) 을 포함함

#### Address → Zipcode
- `Address`는 `Zipcode`라는 또 다른 값 타입(Value Object) 과 연관됨
- 이 관계는 값 타입 ↔ 값 타입 간 연관관계로서 비영속적 관계이며, 단순한 내포(Nested Embedding) 로 해석됨

#### PhoneNumber → PhoneEntity
- `PhoneNumber`는 `PhoneEntity`라는 엔티티(Entity) 와 연관됨
- 이는 값 타입이 엔티티를 참조하는 관계
- 영속성 전이(cascade) 및 연관관계 매핑(join column 등) 관리가 필요

|관계 유형|예시|특징|
|:---|:---|:---|
|엔티티 → 값 타입|Member → Address|일반적인 @Embedded 관계|
|값 타입 → 값 타입|Address → Zipcode|중첩 임베디드 (값 타입 내 값 타입)|
|값 타입 → 엔티티|PhoneNumber → PhoneEntity|연관관계 매핑 필요, 실무에서 신중히 사용|

### 2.6 @AttributeOverride : 속성 재정의
#### 언제 사용하는가?
하나의 엔티티에서 같은 값 타입(예: Address)을 두 번 이상 사용할 경우 (예: homeAddress, workAddress)

#### 해결 방법
- `@AttributeOverride` 또는 `@AttributeOverrides` 어노테이션 사용
- 컬럼 이름을 재정의하여 각각 구분 가능하도록 함

```java
// 주소
@Embedded
@AttributeOverrides({
    @AttributeOverride(name="city", column=@Column(name="WORK_CITY")),
    @AttributeOverride(name="street", column=@Column(name="WORK_STREET")),
    @AttributeOverride(name="zipcode", column=@Column(name="WORK_ZIPCODE"))
})
private Address workAddress;
```

### 2.7 임베디드 타입과 null
임베디드 타입이 `null`이면, 해당 타입이 매핑한 모든 컬럼이 null로 저장됨

예: `Address`가 `null`이면 `city`, `street`, `zipcode` 컬럼 모두 `null`이 됨

---
## 3. 값 타입과 불변 객체
> "값 타입은 복잡한 객체 세상을 조금이라도 단순화하려고 
만든 개념이다. 따라서 값 타입은 단순하고 안전하게 다
룰 수 있어야 한다."

### 3.1 값 타입 공유 참조

<img width="430" height="155" alt="image" src="https://github.com/user-attachments/assets/aa967fbf-a636-4e24-9209-d27d3573d342" />

임베디드 타입을 여러 엔티티에서 공유하면 위험함

하나의 인스턴스를 공유하면 한쪽 변경이 다른 쪽에 영향을 줄 수 있음 → 부작용(side effect) 발생

#### 예시 상황
회원1과 회원2가 같은 Address 객체를 참조

회원2가 city를 "NewCity"로 변경하면, 회원1의 주소도 같이 변경됨 (의도치 않음)

### 3.2 값 타입 복사

<img width="453" height="172" alt="image" src="https://github.com/user-attachments/assets/f91b8b11-6a82-4d58-89d4-3fc165e11744" />

값 타입 복사로 해결!
- 값 타입은 불변 객체로 설계하는 것이 좋음
- 객체를 공유하지 말고 새로운 인스턴스로 복사해서 사용

#### 해결 방법

```java
Address address=new Address("city", "street", "10000");

Member member1=new Member();
member1.setUsername("member1");
member1.setHomeAddress(address);
em.persist(member1);

Address copyAddress=new Address(address.getCity(), address.getStreet(), address.getZipcode());

Member member2=new Member();
member2.setUsername("member2");
member2.setHomeAddress(copyAddress);
em.persist(member2);
            
member1.getHomeAddress().setCity("newCity");

tx.commit();
```

이렇게 하면 member1만 바뀌고 member2는 바뀌지 않음


### 3.3 객체 타입의 한계
- 임베디드 타입 등 자바의 객체 타입은 값을 직접 대입할 수 없음
- 기본 타입은 복사가 되지만, 객체는 참조 전달

따라서
- 객체 타입은 공유 참조를 피할 수 없음
- 객체 타입은 직접 값을 대입하는 방법이 없음
- → 항상 복사해서 사용해야 부작용 방지 가능


#### 기본 타입 (primitive type)
- 값을 복사해서 대입
- 서로 독립적임
```java
int a = 10;
int b = a;   // b는 a의 값 복사
b = 4;       // b만 변경됨, a는 여전히 10
```

#### 객체 타입 (참조 타입)
- 참조(주소)를 전달하여 공유됨
- 한 객체의 변경이 다른 객체에 영향을 줌
```java
Address a = new Address("Old");
Address b = a;         // b는 a와 같은 객체를 참조
b.setCity("New");      // a의 city도 함께 변경됨!
```

### 3.4 불변 객체
한 번 생성되면 절대 상태(값)를 변경할 수 없는 객체. 생성 시점 이후 값 변경 불가

예: 자바에서 제공하는 대표적인 불변 객체는 `String`, `Integer`

#### 왜 불변 객체로 생성?
- 부작용 차단 : 객체 타입을 수정할 수 없게 만들어 예상치 못한 부작용(side effect) 방지
- 안정성 : 값 타입이 여러 곳에서 공유되어도 값 변경이 없어 안정적
- 값 타입에 적합 : 값 타입은 본질적으로 데이터만을 표현하므로, 불변으로 두는 것이 이상적

#### 불변 객체로 만드는 법
- Setter를 만들지 않음
- 생성자(Constructor)로만 초기값을 설정
- 모든 필드에 final 또는 private 지정

> 불변이라는 작은 제약으로 부작용이라는 큰 재앙을 막을 수 있다.

---

## 4. 값 타입의 비교
#### 기본 타입 비교 (Primitive Type)
값 타입은 인스턴스가 다르더라도 그 안의 값이 같으면 같은 것으로 봄.

```java
// 기본 타입 비교 (Primitive Type)
int a = 10;
int b = 10;

System.out.println("a == b: " + (a == b)); //true
```

#### 객체 타입 비교 (Reference Type)
두 인스턴스는 각각 다른 메모리 주소를 가짐

```java
// 객체 타입 비교 (Reference Type)
Address address1 = new Address("city", "street", "10000");
Address address2 = new Address("city", "street", "10000");


System.out.println("address1 == address2: " + (address1 == address2));
System.out.println("address1 equals address2: " + (address1.equals(address2))); // 동등성 (equivalence) 비교
```

Address에서 `equals()`를 (자동으로) 생성(재정의) 

#### 값 타입의 비교 방법
|구분|설명|
|:---|:---|
|동일성 (identity) 비교|인스턴스의 참조 값 비교 → == 사용|
|동등성 (equivalence) 비교|인스턴스의 값 비교 → equals() 메서드 사용|

#### 주의사항
- 값 타입은 반드시 `a.equals(b)` 형태로 동등성 비교를 수행해야 함.
- equals() 메서드는 직접 재정의해서 모든 필드 값 비교하도록 구현해야 정확한 동등성 판단 가능

---
## 5. 값 타입 컬렉션
### 5.1 값 타입 컬렉션

<img width="452" height="357" alt="image" src="https://github.com/user-attachments/assets/c297c7f6-7b4b-4f31-b37a-7f4f732e9e2d" />

하나 이상의 값 타입을 저장할 때 사용됨 (값 타입을 컬렉션에 담아서 쓰는 것)

예: `Set<String> favoriteFoods`, `List<Address> addressHistory`

#### 사용 방법
`@ElementCollection`, `@CollectionTable` 애너테이션 사용

```java
@ElementCollection
@CollectionTable(name = "FAVORITE_FOOD", joinColumns = @JoinColumn(name = "MEMBER_ID"))
@Column(name = "FOOD NAME") //Set은 하나라서 특이하게 맵핑 허용
private Set<String> favoriteFoods = new HashSet<>();

@ElementCollection
@CollectionTable(name = "ADDRESS", joinColumns = @JoinColumn(name = "MEMBER_ID"))
private List<Address> addressHistory = new ArrayList<>();
```

#### 데이터베이스 처리
- RDB는 컬렉션을 한 테이블에 저장할 수 없음
- 따라서 컬렉션마다 별도의 테이블이 필요함
  - `FAVORITE_FOOD`, `ADDRESS` 테이블이 각각 존재
  - 이 테이블들은 `MEMBER_ID`를 외래키로 가짐
    
    
### 5.2 값 타입 컬렉션 사용
#### 값 타입 저장 예제

```java
Member member = new Member();
member.setUsername("member1");
member.setHomeAddress("homeCity", "street", "10000");

member.getFavoriteFoods("치킨");
member.getFavoriteFoods("족발");
member.getFavoriteFoods("피자");

member.getAddressHistory().add(new Address("old1", "street", "10000"));
 member.getAddressHistory().add(new Address("old2", "street", "10000"));

em.persist(member);

tx.commit();
```

#### 값 타입 조회 예제
값 타입 컬렌션도 지연 로딩 전략 사용
- 값 타입 컬렉션도 기본적으로 지연 로딩(Lazy Loading)
- 실제 접근 시 쿼리가 실행됨
```java
em.persist(member);
            
em.flush();
em.clear();

Member findMember = em.find(Member.class, member.getId());

// 컬렉션들은 다 지연로딩이다
List<Address> addressHistory = findMember.getAddressHistory();
for (Address address : addressHistory){
    System.out.println("address: " + address.getCity());
}

Set<String> favoriteFoods = findMember.getFavoriteFoods();
for (String favoriteFood : favoriteFoods){
    System.out.println("favoriteFood = " + favoriteFood);
}

tx.commit();
```

#### 값 타입 수정 예제
기존 값을 통째로 삭제 후 재삽입하는 방식 사용됨
- 개별 항목 수정이 아닌 전체 교체 방식
```java
// homeCity -> newCity 로 하고싶을 때
// findMember.getHomeAddress().setCity("newCity"); 절대 X. side effect 주의
// 값 타입을 통으로 수정해야함!
Address a = findMember.getHomeAddress();
findMember.setHomeAddress(new Address("newCity", a.getStreet(), a.getZipcode());

// 치킨 -> 한식
findMember.getFavoriteFoods().remove("치킨");
findMember.getFavoriteFoods().add("한식");

// 여기 주의 !
// old1 -> newCity1
findMember.getAddressHistory().remove(new Address("old1", "street", "10000"));
findMember.getAddressHistory().remove(new Address("newCiry1", "street", "10000"));
```

#### 참고
값 타입 컬렉션은 필수적으로 다음 기능이 필요함:
- `Cascade`(영속성 전이)
- 고아 객체 제거(`orphanRemoval = true`)

### 5.3 값 타입 컬렉션의 제약사항
- 식별자 없음 : 값 타입은 엔티티와 달리 고유 식별자가 없음
- 변경 추적 어려움 : 값이 변경되면 어떤 항목이 바뀌었는지 추적이 어려움
- 전체 삭제 및 재저장 : 컬렉션에 변경 사항이 발생하면 관련 데이터를 모두 삭제 후 다시 저장
(주인 엔티티와 연관된 값 타입 컬렉션 전체 삭제 및 재삽입)
- 테이블 설계 주의 : 매핑되는 테이블은 모든 컬럼을 묶어 기본 키 구성 필요
→ null 입력 금지, 중복 저장 금지


### 5.4 값 타입 컬렉션 대안
- 대안 제시 : 값 타입 컬렉션 대신 일대다 관계 고려
- 구현 방법 : 일대다 관계를 위한 별도 엔티티 생성 후 값 타입 포함
- 사용 예 : `AddressEntity` 등
- 처리 방식 : Cascade + 고아 객체 제거 기능으로 값 타입 컬렉션처럼 사용 가능

`AddressEntity`
```java
@Entity
public class AddressEntity {

    @Id
    @GeneratedValue
    private Long id;

    private Address address;
    
}
```
Member에서는 
```java
@OneToMany(cascade = CascadeType.ALL, orphanRemoval = true)
@JoinColumn(name = "MEMBER_ID")
private List<AddressEntity> addressHistory = new ArrayList<>();
```

### 5.5 정리
|항목|엔티티 타입|값 타입|
|:---|:---|:---|
|식별자(ID)|있음|없음|
|생명 주기|독립적으로 관리됨|소유한 엔티티에 의존함|
|공유 가능 여부|가능|공우하면 위험함 → 복사하여 사용해야 안전|
|불변 객체 사용 권장|선택 사항|필수에 가까움 (부작용 방지 목적)|
