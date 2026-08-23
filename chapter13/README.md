# Chapter13. 웹 애플리케이션과 영속성 관리

## 1. 트랜잭션 범위의 영속성 컨텍스트
### 1.1. 스프링 컨테이너의 기본 전략
스프링 컨테이너는 트랜잭션 범위의 영속성 컨텍스트 전략을 기본적으로 사용한다.

트랜잭션의 범위 == 영속성 컨텍스트의 생존 범위

보통 서비스 계층에 `@Transactiona` 어노테이션을 이용해서 트랜잭션을 시작하는데 이 어노테이션이 있으면 호출한 메소드를 실행하기 직전에 **스프링의 트랜잭션 AOP가 먼저 동작한다.**

-> 변경 내용을 데이터베이스에 반영한 후에 데이터베이스 트랜잭션에 커밋한다.

```java
@Controller
class HelloController {

	@Autowired HelloService helloService;
    
    public void hello() {
    	//반환된 member 엔티티는 준영속 상태이다.
    Member member = helloService.logic();
    }
}

@Service
class HelloService {
	
    @PersistenceContext
    EntityManager em;
    
    @Autowired Repository1 repository1;
    @Autowired Repository2 repository2;
    
    //트랜잭션 시작
    @Transactional
    public void logic() {
    
    repository1.hello();
    
    	//member는 영속 상태이다.
    	Member member = repository2.findMember();
    	return member;
    }
    //트랜잭션 종료
}
```

1. 조회한 엔티티가 트랜잭션 범위 안에 있으면 영속성 컨텍스트의 관리를 받는다. **영속 상태**
2. @Transactional을 선언한 메소드가 정상 종료되면 트랜잭션을 커밋하는데, 이때 영속성 컨텍스트가 종료된다. -> 조회한 엔티티는 **준영속 상태**가 된다.

위 예시 코드는 서비스 메소드가 끝나면서 트랜잭션과 영속성 컨텍스트가 종료되었으므로 컨트롤러에 반환된 엔티티는 준영속 상태이다.

- 트랜잭션이 같으면 같은 영속성 컨텍스트를 사용한다.
- 트랜잭션이 다르면 다른 영속성 컨텍스트를 사용한다. (같은 엔티티 매니저를 사용해도 트랜잭션에 따라 접근하는 영속성 컨텍스트가 다르다.)
    
## 2. 준영속 상태와 지연 로딩
트랜잭션이 종료되면서
- 서비스나 레포지토리 계층 : 영속 상태 유지
- 컨트롤러나 뷰 같은 프리젠테이션 계층 : 준영속 상태 -> 변경 감지, 지연 로딩 동작 X

#### < 준영속 상태와 변경 감지 >

변경 감지 기능은 서비스 계층(트랜잭션 범위)까지만 동작한다.
어차피 단순히 데이터를 보여주는 프리젠테이션 계층에서는 변경이 거의 없으므로 문제가 되지는 않는다.

#### < 준영속 상태와 지연 로딩 >

준영속 상태에서 지연 로딩을 시도하면 문제가 발생한다.

해결 방법
- 뷰가 필요한 엔티티를 미리 로딩해두는 방법: 영속성 컨텍스트가 살아있을 때 뷰에 필요한 엔티티들을 미리 다 로딩하거나 초기화해서 반환하는 방법
- OSIV를 사용해서 엔티티를 항상 영속 상태로 유지하는 방법

뷰가 필요한 엔티티를 미리 로딩해두는 방법

1. 글로벌 페치 전략 수정
2. JPQL 페치 조인
3. 강제로 초기화

### 2.1. 글로벌 페치 수정

즉시 로딩 설정 : `(fetch = FetchType.EAGER)`
```java
@Entity
public class Order {
	
    @Id
    @GeneratedValue
    private Long id;
    
    @ManyToOne(fetch = FetchType.EAGER) //즉시 로딩 전략
    private Member member;
    ...
}
```

프레젠테이션 로직
```java
Order order = orderService.findOne(orderId);
Member member = order.getMember();
member.getName(); //이미 로딩된 엔티티
```

#### 글로벌 페치 전략에 즉시 로딩 사용 시 단점
- 사용하지 않는 엔티티를 로딩한다.
- N+1 문제가 발생한다. -> JPQL 페치 조인으로 해결 가능 !

### 2.2. JPQL 페치 조인
페치 조인 사용 전
```text
JPQL : select o from Order o
SQL  : select * from Order
```

페치 조인 사용 후
```text
JPQL : 
	select o
    from Order o
    join fetch o.member

SQL  : 
	select o.*, m.*
    from Order o
    join Member m on o.MEMBER_ID=m.MEMBER_ID
```

N+1 문제가 발생하지 않는다!!

#### JPQL 페치 조인의 단점
무분별하게 사용하면 레포지토리 메소드가 증가할 수 있다. 프리젠테이션 계층이 데이터 접근 계층을 침범..

적당한 타협점 찾기 !

### 2.3. 강제로 초기화
영속성 컨텍스트가 살아있을 때 프리젠테이션 계층이 필요한 엔티티를 강제로 초기화해서 반환하는 방법

프록시 강제 초기화
```java
class OrderService {
	
    @Transactional
    public Order findOrder(id) {
    	Order order = orderRepository.findOrder(id);
        order.getMember().getName(); //프록시 객체를 강제로 초기화한다.
        return order;
    }
}
```

이것도 은근 슬쩍 프리젠테이션 계층이 서비스 계층을 침범한다.

-> 서비스 계층에서 프리젠테이션 계층을 위한 프록시 초기화 역할을 분리해야한다 ! FACADE 계층이 그 역할을 담당해준다.

### 2.4. FACADE 계층 추가
FACADE 계층 : 서비스 계층과 프리젠테이션 계층 사이의 논리적인 의존성을 분리한다.

프록시를 초기화하려면 영속성 컨텍스트가 필요하므로 FACADE에서 트랜잭션을 시작해야 한다.

#### FACADE 계층의 역할과 특징
- 프리젠테이션 계층과 도메인 모델 계층 간의 논리적 의존성을 분리해준다.
- 프리젠테이션 계층에서 필요한 프록시 객체를 초기화한다.
- 서비스 계층을 호출해서 비즈니스 로직을 실행한다.
- 레포지토리를 직접 호출해서 뷰가 요구하는 엔티티를 찾는다.

```java
class OrderFacade {

	@Autowired
    OrderService orderService;
    
    public Order findOrder(id) {
    	Order order = orderService.findOrder(id);
        //프리젠테이션 계층이 필요한 프록시 객체를 강제로 초기화
        order.getMember().getName();
        return order;
    }
}

class OrderService {
	
    public Order findOrder(id) {
    	return orderRepository.findOrder(id);
    }
}
```

계층이 하나 더 끼어들어서 결국 코드가 더 많아진다는 게 단점이다..

위의 방법들도 문제점이 존재한다. 결국 모든 문제는 엔티티가 프리젠테이션 계층에서 준영속 상태이기 때문에 발생하는 것이므로 영속성 컨텍스트를 뷰까지 살아있게 열어두자 ! -> OSIV

---
## 3. OSIV
> **OSIV** (Open Session In View) : 영속성 컨텍스트를 뷰까지 열어둔다. -> 뷰에서도 지연로딩을 사용할 수 있게 된다.

### 3.1. 과거 OSIV : 요청 당 트랜잭션
클라이언트의 요청이 들어오자마자 서블릿 필터나 스프링 인터셉터에서 트랜잭션을 시작하고 요청이 끝날 때 트랜잭션도 끝내는 것

#### 요청 당 트랜잭션 방식의 OSIV 문제점
- 프리젠테이션 계층이 엔티티를 변경할 수도 있다.

#### 프리젠테이션 계층에서 엔티티를 수정하지 못하게 막는 방법

- 엔티티를 읽기 전용 인터페이셔로 제공
```java
inteface MemberView {
	public String getName();
}

@Entity
class Member implements MemberView {
	...
}

class MemberService {
	public MemberView getMember(id) {
    	return memberRepository.findById(id);
    }
}
```

- 엔티티 레핑
```java
class MemberWrapper {
	
    private Member member;
    
    public MemberWrapper(member) {
    	this.member = member;
    }
    
    //읽기 전용 메소드만 제공
    public String getName() {
    	member.getName();
    }
}
```

- DTO만 반환
```java
class MemberDTO {
	
    private String name;
    
    //Getter, Setter
}
...
MemberDTO memberDTO = new MemberDTO();
memberDTO.setName(member.getName());
return memberDTO;
```
### 3.2. 스프링 OSIV : 비즈니스 계층 트랜잭션
#### 스프링 프레임워크가 제공하는 OSIV 라이브러리

- 하이버네이트 OSIV 서블릿 필터
`org.springframework.orm.hibernate4.support.OpenSessionInViewFilter`
- 하이버네이트 OSIV 스프링 인터셉터
`org.springframework.orm.hibernate4.support.OpenSessionInViewInterceptor`
- JPA OEIV 서블릿 필터
`org.springframework.orm.jpa.support.OpenEntityManagerInViewFilter`
- JPA OEIV 스프링 인터셉터
`org.springframework.orm.jpa.support.OpenEntityManagerInViewIntereceptor`

#### 스프링 OSIV 분석

1. 클라이언트의 요청이 들어오면 서블릿 필터나 스프링 인터셉터에서 영속성 컨텍스트를 생성한다.
2. 서비스 계층에서 `@Transactional`로 트랜잭션을 시작할 때, 1번에서 생성한 영속성 컨텍스트를 찾아와서 트랜잭션을 시작한다.
3. 서비스 계층이 끝나면 트랜잭션을 커밋하고, 영속성 컨텍스트를 플러시한다. (트랜잭션은 끝내지만 영속성 컨텍스트는 종료하지 X)
4. 컨트롤러와 뷰까지 영속성 컨텍스트가 유지되므로 조회한 엔티티는 영속 상태를 유지한다.
5. 서블릿 필터나 스프링 인터셉터로 요청이 돌아오면 영속성 컨텍스트를 종료한다. (이때 플러시를 호추랗지 않고 바로 종료)

#### 트랜잭션 없이 읽기

스프링이 제공하는 비즈니스 계층 트랜잭션 OSIV 는
- 영속성 컨텍스트를 프리젠테이션 계층까지 유지한다.
- 프리젠테이션 계층에는 트랜잭션이 없으므로 엔티티를 수정할 수 없다.
- 프리젠테이션 계층에는 트랜잭션이 없지만 트랜잭션 없이 읽기를 사용해서 지연 로딩을 할 수 있다. (트랜잭션 없이 읽기)

#### 스프링 OSIV 주의사항
프리젠테이션 계층에서 인티티를 수정한 직후에 트랜잭션을 시작하는 서비스 계층을 호출하면 문제가 발생한다.

-> 트랜잭션이 있는 비즈니스 로직을 모두 호출하고 나서 엔티티를 변경하면 해결된다.
```java
memberService.biz(); //비즈니스 로직 먼저 실행

Member member = memberService.getMember(id);
member.setName("XXX"); //마지막에 엔티티를 수정한다.
```

---

## 4. 너무 엄격한 계층
상품을 구매한 후에 구매 결과 엔티티를 조회하려고 컨트롤러에서 레포지토리에 직접 접근
```java
class OrderController {

	@Autowired OrderService orderservice;
    @Autowired OrderRepository orderRepository;
    
    public String orderRequest(Order order, Model model) {
    	long Id = orderService.order(order); //상품 구매
        
        //레포지토리 직접 접근
        Order orderResult = orderRepository.findOne(id);
        model.addAttribute("order", orderResult);
        ...
    }
}

@Transactional
class OrderService {

	@Autowired OrderRepository orderRepository;
    
    public Long order(order) {
    	...비즈니스 로직
        return orderRepository.save(order);
	}
}

class OrderRepository{

	@PersistenceContext EntityManager em;
    
    public Order findOne(Long id) {
    	return em.find(Order.class, id);
    }
}
```

OSIV를 사용하면 영속성 컨텍스트가 프리젠테이션 계층까지 살아있으므로 미리 초기화할 필요가 없다. 따라서 단순한 엔티티 조회는 컨트롤러에서 레포지토리를 직접 호출해도 문제가 없다.
