# Chapter14. JPA가 지원하는 컬렉션과 부가 기능

1. 컬렉션 : 다양한 컬렉션과 특징
2. 컨버터 : 엔티티의 데이터를 변환에서 데이터베이스에 저장
3. 리스너 : 엔티티에서 발생한 이벤트를 처리
4. 엔티티 그래프 : 엔티티를 조회할 때 연관된 엔티티들을 선택해서 함께 조회

## 1. 컬렉션
JPA는 Collection, List, Set, Map 컬렉션을 지원한다.

컬렉션을 사용하는 경우
- @OneToMany, @ManyToMany 를 사용해서 일대다, 다대다 엔티티 관계를 매핑할 때
- @ElementCollection을 사용해서 값 타입을 하나 이상 보관할 때

<img width="619" height="209" alt="image" src="https://github.com/user-attachments/assets/c1517113-27aa-4def-adcb-14c006d36c14" />

| 컬렉션 유형         | 특징               | 중복 허용                           | 순서 보관                               | 비고                           |
| -------------- | ---------------- | ---------------------------------- | -------------------------------------- | ---------------------------- |
| **Collection** | 자바 최상위 컬렉션 인터페이스 | O                           | X                            | 하이버네이트는 중복 허용 + 순서 비보장으로 가정  |
| **Set**        | 집합 구조            | X                       | X                             | 예: `HashSet`, `TreeSet`      |
| **List**       | 순차적 구조           | O                          | O                                | 예: `ArrayList`, `LinkedList` |
| **Map**        | Key-Value 구조     | Key: X <br>Value: O | X (일반적 비보장, 단 `LinkedHashMap` 등 예외 있음) | 특수한 컬렉션                      |

### 1.1. JPA와 컬렉션
하이버네이트는 엔티티를 영속 상태로 만들 때 컬렉션 필드를 하이버네이트에서 준비한 컬렉션으로 감싸서 사용한다.
```java
@Entity
public class Team {
	
    @Id
    private String id;
    
    @OneToMany
    @JoinColumn
    private Collection<Member> members = new ArrayList<Member>();
    ...
    
}
```
원래 ArrayList 타입이었던 컬렉션이 엔티티를 영속 상태를 만든 직후 하이버네이트가 제공하는 PersistentBag 타입으로 변경된다.

인터페이스에 따라 어떤 래퍼 컬렉션이 사용되는지 !
```java
//org.hibernate.collection.internal.PersistentBag
@OneToMany
Collection<Member> collection = new ArrayList<Member>();

//org.hibernate.collection.internal.PersistentBag
@OneToMany
List<Member> list = new ArrayList<Member>();

//org.hibernate.collection.internal.PersistentSet
@OneToMany
Set<Member> set = new HashSet<Member>();

//org.hibernate.collection.internal.PersistentList
@OneToMany @OrderColumn
List<Member> orderColumnList = new ArrayList<Member>();
```

|컬렉션 인터페이스|내장 컬렉션|중복 허용|순서 보관|
|:---|:---|:---|:---|
Collection, List|PersistentBag|O|X
Set|PersistentSet|X|X
List  + @OrderColumn|PersistentList|O|O

### 1.2. Collection, List
Collection, List은 PersistentBag을 래퍼 컬렉션으로 사용
```java
@OneToMany
@JoinColumn
private Collection<CollectionChild> collection = new ArrayList<CollectionChild>();

@OneToMany
@JoinColumn
private List<ListChild> list = new ArrayList<ListChild>();
```

Collection과 List는 중복을 허용하므로 객체를 추가하는 `add()` 메서드는 항상 true

같은 엔티티가 있는지 찾거나 삭제할 때는 `equals()` 메서드 사용
```java
List<Comment> comments = new ArrayList<Comment)();
...
//단순히 추가만 한다. 결과는 항상 true!
boolean result = comments.add(data)

comments.contains(comment); //equals 비교
comments.remove(comment); //equals 비교
```

### 1.3. Set
Set은 PersistentSet을 래퍼 컬렉션으로 사용한다.
```java
@OneToMany
@JoinColumn
private Set<SetChild> set = new HashSet<SetChild>();
```

HashSet은 중복을 허용하지 않으므로 객체를 추가하기 전에 같은 객체가 있는지 비교해야한다.
```java
Set<Comment> comments = new HashSet<Comment>();
...
boolean result = comments.add(data) //hashcode + equals 비교. 같은 객체가 있으면 false 반환
comments.contains(comment); //hashcode + equals 비교
comments.remove(comments); //hashcode + equals 비교
```

Set은 엔티티를 추가할 때마다 중복된 엔티티가 있는지 비교해야하므로 엔티티를 추가할 때 지연 로딩된 컬렉션을 초기화한다 !

### 1.4. List + @OrderColumn
List + @OrderColumn : List에 순서 추가
List는 PersistentList를 래퍼 컬렉션으로 사용한다.
```java
@OneToMany(mappedBy = "board")
@OrderColumn(name = "POSITION")
private List<Comment> comments = new ArrayList<Comment>();
```

자바가 제공하는 List 컬렉션은 내부에 위치 값을 가지고 있다.
```java
list.add(1, data1); //1번 위치에 data1을 저장
list.get(10); //10번 위치에 있는 값을 조회
```

순서가 있는 컬렉션은 데이터베이스에 순서 값도 함께 관리한다!

#### @OrderColumn 의 단점
- @OrderColumn을 Board 엔티티에서 매핑하므로 Comment는 POSITION의 값을 알 수 없다. 즉, POSITION 값을 UPDATE 하는 SQL이 추가로 발생한다.
- List를 변결하면 연관된 많은 위치 값을 변경해야 한다.
- 중간에 POSITION 값이 없으면 조회한 List에는 null이 보관된다. 즉, 컬렉션을 순회할 때 NullPointerException이 발생할 수 있다.

### 1.5. @OrderBy
데이터베이스의 ORDER BY절을 사용해서 컬렉션을 정렬할 수도 있다.
- 순서용 컬럼을 매핑할 필요도 없고
- 모든 컬렉션에 사용할 수 있다
```java
@OneToMany(mappedBy = "team")
@OrderBy("username desc, id asc")
private Set<Member> members = new HashSet<Member>();
```

하이버네이트는 Set에 @OrderBy를 적용해서 결과를 조회하면 순서를 유지하기 위해 HashSet 대신에 LinkedHashSet 을 내부에서 사용한다.

---

## 2. @Converter
> **컨버터 (Converter)** : 엔티티의 데이터를 변환해서 데이터베이스에 저장할 수 있게 한다.

예를 들면 참/거짓 (boolean)을 Y/N(문자)로 저장하고 싶을 때

회원 엔티티
```java
@Entity
public class Member {
	
    @Id
    private String id;
    private String username;
    
    @Converter(converter = BooleanToYNConverter.class)
    private boolean vip;
    
    //Getter, Setter
    ..
    
}
```

#### Boolean을 YN으로 변환해주는 컨버터
```java
@Converter
public class BooleanToYNConverter implements AttributeConverter<Boolean, String> {
	
    @Override
    public String converterToDatabaseColumn(Boolean attribute) {
    	return (attribute != null && attribute) ? "Y" : "N";
    }
    
    @Override
    public Boolean convertToEntityAttribute(String dbData) {
    	return "Y".equals(dbData);
    }
}
```

#### AttributeConverter
```java
public interface AttributeConverter<X, Y> {
	
    public Y convertToDatabaseColumn (X attribute);
    public X convertToEntityAttribute (Y dbData);
}
```

- convertToDatabaseColumn() : 엔티티의 데이터를 데이터베이스 컬럼에 저장할 데이터로 변환한다.
- convertToEntityAttribute() : 데이터베이스에서 조회한 컬럼 데이터를 엔티티의 데이터로 변환한다.

#### 컨버터 클래스 레벨에 설정하기

근데 이때 attributeName 속성을 사용해서 어떤 필드에 컨버터를 적용할지 명시해야한다.
```java
@Entity
@Convert(converter = BooleanToYNConverter.class, attributeName = "vip")
public class Member {
	
    @Id
    private String id;
    private String username;
    
    private boolean vip;
    ...
    
}
```

#### 글로벌 설정

모든 Boolean 타입에 컨버터를 적용하려면 `@Converter(autoApply = true)` 옵션을 적용하면 된다.
```java
@Converter(autoApply = true)
public class BooleanToYNConverter implements AttributeConverter<Boolean, String> {
	
    ...
    
}
 ```
 그러면 @Converter를 지정하지 않아도 모든 Boolean 타입에 대해 자동으로 컨버터가 적용된다.
 ```java
 @Entity
 public class Member {
 
 	@Id
    private String id;
    private String username;
    
    private boolean vip; //가능!
    
    ...
}
```

#### Converter 속성 정리

|속성|기능|기본값|
|:---|:---|:---|
|Converter|사용할 컨버터를 지정한다.||
|attributeName|컨버터를 적용할 필드를 지정한다.
|disableConversion|글로벌 컨버터나 상속 받은 컨버터를 사용하지 않는다.|false|

---

## 3. 리스너
JPA 리스너를 사용하면 엔티티의 생명주기에 따른 이벤트를 처리할 수 있다.

### 3.1. 이벤트 종류
이벤트 발생 시점

#### `@PostLoad`
- 엔티티가 영속성 컨텍스트에 조회된 직후 발생 (EntityManager.find(), JPQL 조회, refresh() 호출 등)
- 2차 캐시에 있어도 호출된다.

#### `@PrePersist`
- persist() 호출 → 엔티티를 영속성 컨텍스트에 등록 직전 발생
- 식별자 생성 전략 사용 시 아직 ID 없음
- merge()로 새로운 엔티티 저장할 때도 실행된다.

#### `@PostPersist`
- flush 또는 commit → DB에 실제 저장 직후 발생
- IDENTITY 전략이면 persist() 직후 DB 저장되므로 이때 바로 실행된다.

#### `@PreUpdate`
- flush 또는 commit → DB에 업데이트 직전 발생
- 변경 감지(Dirty Checking)로 수정 SQL이 실행되기 전에 호출한다.

#### `@PostUpdate`
- flush 또는 commit → DB에 업데이트 직후 발생
- 실제 UPDATE 쿼리 수행 완료 후 실행된다.

#### `@PreRemove`
- remove() 호출 → 엔티티를 영속성 컨텍스트에서 제거 직전에 발생
- 연관 엔티티 삭제(영속성 전이, cascade remove) 시에도 실행된다.
- orphanRemoval=true 는 flush/commit 시점에 실행된다.

#### `@PostRemove`
- flush 또는 commit 시 → DB에서 삭제 직후 발생
- 실제 DELETE SQL 수행 완료 후 실행된다.

정리하자면

\- Pre 이벤트 (PrePersist, PreUpdate, PreRemove) → DB 반영 전에 실행
\- Post 이벤트 (PostPersist, PostUpdate, PostRemove) → DB 반영 직후 실행
\- PostLoad → 조회 직후 실행

### 3.2. 이벤트 적용 위치
#### 1) 엔티티에 직접 적용

이벤트를 엔티티에 직접 적용한다.
```java
@Entity
public class Duck {

	@Id @GeneratedValue
	private Long id;

	private String name;

	@PrePersist
	public void prePersist() {
		System.out.println("Duck.prePersist id=" + id);
	}

	@PostPersist
	public void postPersist() { 
		System.out.println("Duck.postPersist id=" + id);
	}

	@PostLoad
	public void postLoad() {
		 System.out.println("Duck.postLoad");
	}

	@PreRemove
	public void preRemove() {
		 System.out.println("Duck.preRemove");
	}

	@PostRemove
	public void postRemove() {
		 System.out.println("Duck.postRemove");
	}
	...
}
```

#### 2) 별도의 리스너 등록

별도의 리스너를 사용한다.
```java
@Entity
@EntityListeners(DuckListener.class)
public class Duck {
	...
}

public class DuckListener {

	@PrePersist
	// 특정 타입이 확실하면 특정 타입을 받을 수 있다. 
	private void perPersist(Object obj) {
		System.out.println("DuckListener.prePersist obj = [" + obj + "]");
	}

	@PostPersist
	// 특정 타입이 확실하면 특정 타입을 받을 수 있다. 
	private void postPersist(Object obj) {
		System.out.println("DuckListener.postPersist obj = [" + obj + "]");
	}
}
```

#### 3) 기본 리스너 사용

모든 엔티티의 이벤트를 처리하려면 `META-INF/orm.xml` 에 기본 리스너로 등록하면 된다.
```java
<?xml version="1.0" encoding="UTF-8"?>
<entity-mappings ...>
	<persistence-unit-metadata>
		<persistence-unit-defaults>
			<entity-listeners>
				<entity-listener class="jpabook.jpashop.domain.
					test.listener.DefaultListener" />
			</entity-listeners>
		</persistence-unit-defaults>
	<persistence-unit-metadata>

</entity-mappings>
```

여러 리스너를 등록했을 때 이벤트 호출 순서는
**기본 리스너 → 부모 클래스 리스너 → 리스너 → 엔티티**

더 세밀한 설정을 위한 어노테이션
\- `javax.persistence.ExcludeDefaultListeners` : 기본 리스너 무시
\- `javax.persistence.ExcludeSuperclassListeners` : 상위 클래스 이벤트 리스너 무시

---

## 4. 엔티티 그래프
엔티티 그래프 기능 : 엔티티 조회시점에 연관된 엔티티들을 함께 조회하는 기능

### 4.1. Named 엔티티 그래프
`@NamedEntityGrpah` 로 정의한다.
- name : 엔티티 그래프의 이름을 정의한다.
- attributeNodes 함께 조회할 속성을 선택한다. 이때 `@NamedAttributeNode`를 사용하고 그 값으로 함께 조회할 속성을 선택하면 된다.
```java
@NamedEntityGrpah(name = "Order.withMember", attributeNodes = {
    @NamedAttributeNode("member")
})
@Entity
@Table(name = "ORDERS")
public class Order {
    
    @Id	@GeneratedValue
    @Column (name = "ORDER_ID")
    private Long id;
    
    @ManyToOne (fetch = FetchType.LAZY, optional = false)
    @JoinColumn (name = "MEMBER_ID")
    private Member member;
    
    ...
}
```

### 4.2. em.find()에서 엔티티 그래프 사용
Named 엔티티 그래프를 사용하려면`em.getEntityGraph("Order.withMember")` 를 통해서 정의한 엔티티 그래프를 찾아온다.

```java
EntityGraph graph = em.getEntityGraph("Order.withMember");

Map hints = new HashMap();
hints.put("javax.persistence.fetchgraph", graph);

Order order = em.find(Order.class, orderId, hints);
```

### 4.3. subgraph

```java
@NamedEntityGraph(name = "Order.withAll", attributeNodes = {
	@NamedAttributeNode("member"),
	@NamedAttributeNode(value = "orderItems", subgraph = "orderItems")
	},
	subgraphs = @NamedSubgraph(name = "orderItems", attributeNodes = {
		@NamedAttributeNode("item")
	})
)
@Entity
@Table(name = "ORDERS")
public class Order {

	@Id @GeneratedValue
	private Long id;

	@ManyToOne(fetch = FetchTYpe.LAZY, optional = false)
	@JoinCloumn(name = "MEMBER_ID")
	private Member member; //주문 회원

	@OneToMany(mappedBy = "order", cascade = CascadeType.ALL)
	private List<OrderItem> orderItems = new ArrayList<OrderItem>();
	...
}

@Entity
public class OrderItem {

	@Id @GeneratedValue
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "ITEM_ID")
	private Item item; //주문 상품

	...
}
```
Order → Member, Order → OrderItem, OrderItem → Item 의 객체 그래프를 함께 조회한다.

### 4.4. JPQL에서 엔티티 그래프 사용
```java
List<Order> resultList =
	em.createQuery("select o from Order o where o.id = :orderId", Order.class)
		.setParameter("orderId", orderId)
		.setHint("javax.persistence.fetchgraph", em.getEntityGraph("Order.withAll"))
		.getResultList();
```

### 4.5. 동적 엔티티 그래프
`createEntityGraph()` 메소드를 사용한다.

`public <T> EntityGraph<T> createEntityGraph(Class<T> rootType);`

```java
EntityGraph<Order> graph = em.createEntityGraph(Order.class);
graph.addAttributeNodes("member");

Map hints = new HashMap();
hints.put("javax.persistence.fetchgraph", graph);

Order order = em.find(Order.class, orderId, hints);
```

### 4.6. 엔티티 그래프 정리
- ROOT에서 시작 : 엔티티 그래프는 항상 조회하는 엔티티의 ROOT에서 시작해야 한다.

- 이미 로딩된 엔티티 : 영속성 컨텍스트에 해당 엔티티가 이미 로딩되어 있으면 엔티티 그래프가 적용되지 않는다.(아직 초기화되지 않은 프록시에는 엔티티 그래프가 적용된다.)

- fetchgraph, loadgraph 의 차이
  - `javax.persistence.fetchgraph` : 엔티티 그래프에 선택한 속성만 함께 조회한다.
  - `javax.persistence.loadgraph` : 엔티티 그래프에 선택한 속성뿐만 아니라 글로벌 fetch 모드가 FetchType.EAGER로 설정된 연관관계도 포함해서 함께 조회한다.
