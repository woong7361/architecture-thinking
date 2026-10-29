# Task F-0: 이번 Station의 책 (마인드셋 1권 + 기술 + 팀그릿)

(Grit's Why): 동시성은 "정상일 때"가 아니라 "스트레스를 받을 때" 진짜 모습을 드러냅니다. 평온할 때 멀쩡하던 시스템이 부하 아래에서 숨은 결함을 토해내는 구조를, 『안티프래질』의 사고로 먼저 잡습니다.

### 읽기 범위 (3축)

- [마인드셋] 『안티프래질』(나심 탈레브): 스트레스와 무질서가 숨은 취약성을 드러내고, 그 노출을 통해 시스템이 오히려 강해질 수 있다는 대목을 정독하세요. 동시 요청은 코드에 가하는 스트레스 테스트이고, race condition은 평소엔 보이지 않던 프래질리티입니다.
- [기술] 『자바 동시성 프로그래밍』(브라이언 고에츠 외) + 팀그릿 ai-backend-guide Ch5(비관/낙관/분산 락): F-3 직전에 펼치세요. 락 종류별로 무엇을 보장하고 무엇을 비용으로 치르는지가 핵심입니다.
- [팀그릿] harness-engineering-playbook Ch3-5(Gen/Eval 분리 + validate + rubric): F-4의 소스코드 리딩 에이전트와 동시성 검증 파이프라인에 직접 연결됩니다.

### 제출물

- [ ] [안티프래질] 충격 구절이나 개념 세 가지와, 그것이 동시성/장애를 보는 본인 관점을 어떻게 흔들었는지. (최소 400자)
- [ ] [기술서] 비관적 락과 낙관적 락이 각각 무엇을 보장하고 무엇을 비용으로 치르는지, 책 속 설명 1개를 인용해 본인 언어로 정리. (최소 300자)

---

## 답안
### 읽기 범위 정리 — [마인드셋] 『안티프래질』

나심 탈레브, *Antifragile: Things That Gain from Disorder* (Random House, 2012). 아래 인용은 모두 원서에서 확인했고, 위치는 장과 절 제목으로 적었다. 번역서(와이즈베리)의 문구와 쪽수는 다를 수 있다.

**프래질 / 로버스트 / 안티프래질의 3분류.** 탈레브는 깨지는 것(fragile)의 반대말이 "튼튼한 것"이 아니라고 지적하면서 시작한다. 1장 「Half of Life Has No Name」의 소포 비유가 그 논증이다 — 깨지는 소포에는 "취급주의"를 붙이지만, 튼튼한 소포에는 아무것도 붙일 필요가 없다. 논리적으로 "취급주의"의 반대는 **"마구 다뤄 주십시오"**이고, 그 내용물은 안 깨지는 것에 그치지 않고 충격에서 이득을 본다. 그런 단어가 영어에 없어서 안티프래질이라는 신조어를 만들었다.

> "The fragile is the package that would be at best unharmed, the robust would be at best and at worst unharmed. And the opposite of fragile is therefore what is at worst unharmed." (1장)
>
> 프래질한 것은 잘 되어야 무사한 소포이고, 로버스트한 것은 잘 되든 못 되든 무사한 소포다. 그러므로 프래질의 반대는 **최악의 경우에 무사한** 것이다.

프롤로그의 정의가 이 셋을 가른다. "Antifragility is beyond resilience or robustness. The resilient resists shocks and stays the same; the antifragile gets better."(프롤로그 II) — 안티프래질리티는 회복력이나 강건함을 넘어선다. 회복력 있는 것은 충격에 저항해 그대로 남고, 안티프래질한 것은 더 좋아진다. 1장은 이것을 신화로 매핑한다. 다모클레스의 칼(프래질), 불사조(로버스트), 히드라(안티프래질) — "Each time one is cut off, two grow back. So harm is what it likes."(머리를 하나 자르면 둘이 돋는다. 그러니 히드라는 해를 좋아한다.)

**스트레스가 숨은 취약성을 드러낸다 — 그리고 스트레스의 제거가 취약성을 만든다.** 과제가 지정한 대목의 핵심은 여기다. 프롤로그의 「Deprivation of Antifragility」(안티프래질리티의 박탈)가 명제를 세운다.

> "depriving these systems of volatility, randomness, and stressors will harm them. They will weaken, die, or blow up."
>
> 이런 시스템에서 변동성과 무작위성과 스트레스 요인을 빼앗으면 해를 입는다. 약해지거나, 죽거나, 터진다.

5장 「The Souk and the Office Building」이 그 메커니즘을 설명한다. 작은 산불은 주기적으로 가장 잘 타는 연료를 태워 없애 그것이 쌓일 기회를 주지 않는다. "Systematically preventing forest fires from taking place 'to be safe' makes the big one much worse."(안전하자고 산불을 조직적으로 막으면 큰 산불이 훨씬 더 나빠진다.) 같은 이유로 경제의 안정은 좋은 것이 아니다 — 기업은 좌절 없는 번영이 길어질 때 오히려 약해지고, 숨은 취약성이 수면 아래에서 조용히 쌓인다.

이 장의 결론 문단이 가장 중요하다.

> "the problem with artificially suppressed volatility is not just that the system tends to become extremely fragile; it is that, at the same time, it exhibits **no visible risks**. Also remember that **volatility is information**. In fact, these systems tend to be too calm and exhibit minimal variability as silent risks accumulate beneath the surface." (5장 「What to Tell the Foreign Policy Makers」)
>
> 인공적으로 억제된 변동성의 문제는 시스템이 극도로 프래질해진다는 것만이 아니다. 동시에 **눈에 보이는 위험이 사라진다**는 것이다. 그리고 **변동성은 정보**라는 점을 기억하라. 실제로 이런 시스템은 지나치게 조용하고 변동이 최소인 모습을 보이며, 그동안 침묵하는 위험이 수면 아래에 쌓인다.

**칠면조 문제와 증거의 부재.** 같은 5장. 칠면조는 1000일 동안 먹이를 받으며 "정육점 주인은 칠면조를 사랑한다"는 통계적 확신을 키운다. 결정적인 문장은 확신이 최대가 되는 시점의 묘사다 — 믿음이 수정되는 그 순간, 칠면조의 삶은 "very quiet and soothingly predictable"(매우 조용하고 안심이 되도록 예측 가능한) 상태다. 탈레브는 여기서 "the mother of all harmful mistakes"(모든 해로운 오류의 어머니)를 끌어낸다.

> "mistaking absence of evidence (of harm) for evidence of absence"
>
> (해악의) 증거가 없는 것을 해악이 없다는 증거로 착각하는 것.

그래서 과제는 "how not to be a turkey"(칠면조가 되지 않는 법)로 요약되고, 그 출발점은 "the difference between true and manufactured stability"(진짜 안정과 만들어진 안정의 차이)를 구분하는 일이다.

**취약성은 측정되지만 위험은 측정되지 않는다.** 프롤로그 「Nonprediction」과 「On the Measurability of (Some) Things」. 이 대목이 탈레브의 방법론이다.

> "It is far easier to figure out if something is fragile than to predict the occurrence of an event that may harm it. Fragility can be measured; risk is not measurable."
>
> 무언가가 프래질한지 알아내는 것은 그것을 해칠 사건의 발생을 예측하는 것보다 훨씬 쉽다. 취약성은 측정할 수 있다. 위험은 측정할 수 없다.

근거는 취약성이 **현재의 속성**이라는 것이다. "fragility and antifragility are part of the current property of an object, a coffee table, a company, an industry, a country, a political system." 어떤 충격이 다른 충격보다 더 일어날 법하다고는 신뢰성 있게 말할 수 없지만, 어떤 사건이 일어난다면 이 구조물이 저것보다 더 프래질하다고는 훨씬 더 확신을 갖고 말할 수 있다. 판정에 쓰는 도구는 **비대칭성 테스트** 하나다 — "anything that has more upside than downside from random events (or certain shocks) is antifragile; the reverse is fragile."(무작위 사건에서 이득이 손해보다 큰 것은 안티프래질하고, 그 반대는 프래질하다.)

**이아트로제닉스 — 치료자가 입히는 해.** 7장 「Naive Intervention」. 탈레브는 무언가 "해야 한다"는 충동을 naive interventionism(소박한 개입주의)이라 부르고, 그 비용에 이름을 붙인다.

> "the (usually hidden or delayed) damage from treatment in excess of the benefits, is iatrogenics, literally, 'caused by the healer,' iatros being a healer in Greek."
>
> 이득을 초과하는, 보통 숨어 있거나 지연되는 치료의 피해가 이아트로제닉스다. 말 그대로 "치료자가 일으킨"이라는 뜻이고, iatros는 그리스어로 치료자다.

핵심 귀결은 7장 「First, Do No Harm」의 문장이다. "Since no intervention implies no iatrogenics, the source of harm lies in the denial of antifragility, and to the impression that we humans are so necessary to making things function."(개입이 없으면 이아트로제닉스도 없으므로, 해의 근원은 안티프래질리티의 부정에, 그리고 사물이 돌아가는 데 우리 인간이 꼭 필요하다는 착각에 있다.) 평가하기 쉬운 이아트로제닉스(엉뚱한 다리를 절단하는 일)와 회계에 잡히지 않는 이아트로제닉스를 구분하는데, 후자가 더 위험하다.

프롤로그가 이것을 한 문장으로 요약한다. "This is the tragedy of modernity: as with neurotically overprotective parents, those trying to help are often hurting us the most."(이것이 근대의 비극이다. 신경증적으로 과보호하는 부모처럼, 도우려는 자들이 우리를 가장 많이 해치는 경우가 많다.)

**호르메시스 — 참고.** 1장 「Proto-Antifragility」. 적은 용량의 해로운 물질이 과잉반응을 유발해 유기체를 전체적으로 더 낫게 만드는 현상이다. 탈레브는 호르메시스를 안티프래질리티 자체가 아니라 "proto-안티프래질리티"로 분류하고 둘을 구별한다. 제출물에 굳이 안 써도 되지만, "스트레스의 결핍이 해롭다"는 명제의 가장 오래된 근거다 — "hormesis is the norm, and its absence is what hurts us."(호르메시스가 정상이고, 그것의 부재가 우리를 해친다.)

---

### 읽기 범위 정리 — 『안티프래질』 중·후반부

초반부(프롤로그~7장)가 "스트레스가 숨은 취약성을 드러낸다"는 명제를 세운다면, 중·후반부는 그것을 **측정 절차**로 바꾼다. 아래 일곱 장이 그 경로다.

#### 4장 「What Kills Me Makes Others Stronger」 — 부분의 프래질리티가 전체의 안티프래질리티를 만든다

4장의 명제는 안티프래질리티가 공짜가 아니라는 것이다. 누군가의 안티프래질리티는 다른 누군가의 프래질리티를 대가로 치른다.

> "the antifragility of some comes necessarily at the expense of the fragility of others. In a system, the sacrifices of some units—fragile units, that is, or people—are often necessary for the well-being of other units or the whole. The fragility of every startup is necessary for the economy to be antifragile." (4장 「Antifragility by Layers」)
>
> 어떤 것의 안티프래질리티는 필연적으로 다른 것들의 프래질리티를 대가로 온다. 시스템 안에서 일부 단위 — 프래질한 단위, 즉 사람 — 의 희생은 다른 단위나 전체의 안녕을 위해 자주 필요하다. 모든 스타트업의 프래질리티는 경제가 안티프래질하기 위해 필요하다.

탈레브는 자연 유기체가 단일한 최종 단위가 아니라 **하위 단위로 구성되고 그 자신도 더 큰 집합의 하위 단위**라는 계층 구조로 이것을 설명한다. 동시성에 옮기면 낙관적 락의 재시도 구조가 정확히 이 모양이다. 개별 요청이 "충돌했으니 실패했다"고 깨지는 것이 시스템 전체의 정확성을 유지하는 방식이다. 전체를 안 깨지게 만드는 길이 모든 요청을 안 깨지게 만드는 것이라고 가정하는 순간 비관적 락으로 전부 직렬화하게 되고, 그것이 11.4절이 말하는 확장성 붕괴다.

#### 11장 「Never Marry the Rock Star」 — 바벨 전략: 중간을 비운다

바벨은 **양 극단을 분리해 유지하고 중간을 피하는** 구성이다.

> "The barbell ... is meant to illustrate the idea of a combination of extremes kept separate, with avoidance of the middle. ... If you put 90 percent of your funds in boring cash ... and 10 percent in very risky, maximally risky, securities, you cannot possibly lose more than 10 percent, while you are exposed to massive upside. Someone with 100 percent in so-called 'medium' risk securities has a risk of total ruin from the miscomputation of risks."
>
> 바벨은 극단들을 분리해 유지하고 중간을 피한다는 생각을 나타낸다. 자금의 90%를 지루한 현금에 두고 10%를 매우 위험한, 최대로 위험한 증권에 두면 10% 이상은 결코 잃을 수 없으면서 막대한 상승에 노출된다. 100%를 이른바 "중간" 위험 증권에 둔 사람은 위험을 잘못 계산한 탓에 완전한 파산 위험을 진다.

중간을 피하는 근거가 중요하다. "medium risks can be subjected to huge measurement errors"(중간 위험은 거대한 측정 오차에 노출될 수 있다). 그리고 바벨의 정의는 금융에 한정되지 않는다 — "A barbell can be any dual strategy composed of extremes, without the corruption of the middle."(바벨은 중간의 오염 없이 극단들로 구성된 어떤 이중 전략이든 될 수 있다.) 탈레브는 이것을 "aggressiveness plus paranoia—clip your downside, protect yourself from extreme harm, and let the upside ... take care of itself"(공격성 더하기 편집증 — 하방을 잘라내고 극단적 피해로부터 자신을 보호한 뒤, 상방은 알아서 하게 둔다)로 요약한다.

락 설계로 옮기면 "모든 경로에 적당한 락을 고르게 바르는 것"이 바로 중간이다. 바벨 쪽은 **돈과 직결된 소수의 자원에는 가장 보수적인 보장을 걸고, 나머지는 공유를 아예 없애는** 구성이다.

#### 18장 「On the Difference Between a Large Stone and a Thousand Pebbles」 — 프래질리티는 비선형성에서 온다

이 장이 중반부의 중심이다. 왕이 아들을 큰 돌로 눌러 죽이겠다고 맹세한 뒤, 현명한 조언자가 그 돌을 자갈로 쪼개 던지게 한 이야기(Midrash Tehillim)에서 출발한다. 같은 무게인데 피해가 다른 이유가 비선형성이다.

> "For the fragile, shocks bring higher harm as their intensity increases (up to a certain level)."
>
> 프래질한 것에게 충격은 강도가 커질수록 더 큰 피해를 가져온다 (어느 수준까지는).

괄호 안의 단서가 중요하다. 이것이 "문턱" 조건의 원문 근거다. 뒤집은 쪽도 같은 형식으로 적혀 있다 — "For the antifragile, shocks bring more benefits (equivalently, less harm) as their intensity increases (up to a point)."(안티프래질한 것에게 충격은 강도가 커질수록 더 큰 이득, 같은 말로 더 적은 피해를 가져온다. 어느 지점까지는.) 역기 비유가 그 예다. 100파운드를 한 번 드는 것이 50파운드를 두 번 드는 것보다 이득이 크고, 1파운드를 백 번 드는 것보다는 훨씬 더 크다.

자동차 예시가 가장 선명하다.

> "Your car is fragile. If you drive it into the wall at 50 miles per hour, it would cause more damage than if you drove it into the same wall ten times at 5 mph. The harm at 50 mph is more than ten times the harm at 5 mph."
>
> 당신의 자동차는 프래질하다. 시속 50마일로 벽에 박으면 같은 벽에 시속 5마일로 열 번 박는 것보다 더 큰 피해가 난다. 시속 50마일의 피해는 시속 5마일 피해의 열 배보다 크다.

**왜 프래질리티가 비선형에 있는가**에 대한 논증이 이 장의 핵심이고, 생존 확률의 구조로 설명한다.

> "conditional on something being unharmed (or having survived), then it is more harmed by a single rock than a thousand pebbles, that is, by a single large infrequent event than by the cumulative effect of smaller shocks."
>
> 무언가가 아직 멀쩡하다는(혹은 살아남았다는) 조건 아래에서, 그것은 자갈 천 개보다 돌 하나에 더 상한다. 즉 작은 충격들의 누적 효과보다 드물고 큰 단일 사건에 더 상한다.

근거는 귀류법이다. 1밀리미터 점프가 10미터 점프 피해의 정확한 선형 비율만큼 해를 입힌다면, 그 사람은 거실을 걸어다니는 것만으로 몇 시간 안에 죽었을 것이다. 그래서 결론이 이렇다. "what is fragile is something that is both unbroken and subjected to nonlinear effects"(프래질한 것은 아직 깨지지 않았으면서 비선형 효과에 노출된 것이다.)

부하 테스트 설계에 바로 들어온다. 동시 10 요청을 열 번 돌린 것과 동시 100 요청을 한 번 돌린 것은 같은 테스트가 아니다. 프래질한 쪽은 후자에서만 깨지고, 그것이 운영에서 실제로 오는 모양이다.

#### 19장 「The Philosopher's Stone and Its Inverse」 — 취약성 탐지 휴리스틱: 가속을 측정한다

19장은 18장의 비선형성을 **절차**로 만든다. 출발점은 2003년 패니메이의 내부 위험 보고서다. 탈레브가 본 것은 이것이다.

> "a move upward in an economic variable led to massive losses, a move downward (in the opposite direction), to small profits. Further moves upward led to even larger additional losses and further moves downward to even smaller profits. ... Acceleration of harm was obvious."
>
> 어떤 경제 변수가 위로 움직이면 막대한 손실이, 반대 방향으로 아래로 움직이면 작은 이익이 났다. 더 위로 움직이면 추가 손실이 더욱 커지고, 더 아래로 움직이면 이익은 더욱 작아졌다. 피해의 가속이 명백했다.

그래서 방법 전체가 한 문장으로 줄어든다.

> "It all boils down to the following: figuring out if our miscalculations or misforecasts are on balance more harmful than they are beneficial, and how accelerating the damage is."
>
> 전부 다음으로 귀결된다 — 우리의 오산이나 오예측이 총합에서 이로운 것보다 해로운지, 그리고 그 피해가 얼마나 가속하는지를 알아내는 것.

그리고 책이 **fragility (and antifragility) detection heuristic**(취약성·안티프래질리티 탐지 휴리스틱)이라 부르는 절차를 교통량 예시로 제시한다.

> "Say you measure that when traffic increases by ten thousand cars, travel time grows by ten minutes. But if traffic increases by ten thousand more cars, travel time now extends by an extra thirty minutes. Such acceleration of traffic time shows that traffic is fragile and you have too many cars and need to reduce traffic until the acceleration becomes mild."
>
> 교통량이 1만 대 늘면 통행 시간이 10분 늘어난다고 측정했다 치자. 그런데 교통량이 1만 대 더 늘면 통행 시간이 이제 추가로 30분 늘어난다. 이런 통행 시간의 가속은 교통이 프래질하다는 것을, 차가 너무 많다는 것을, 그리고 가속이 완만해질 때까지 교통량을 줄여야 한다는 것을 보여 준다.

이것이 그대로 동시성 벤치마크의 판정 기준이다. 동시 요청을 50 → 100 → 150으로 올리며 p99 지연의 **증가분이 커지는지**를 본다. 증가분이 일정하면 선형이고, 커지면 그 자원이 프래질하다. 평균 처리량 한 숫자만 보면 이 가속이 보이지 않는다. 같은 장은 이것을 기업에도 적용한다 — "Should sales increase 10 percent, then profits would increase less than they would decrease should sales drop 10 percent."(매출이 10% 늘면 이익은, 매출이 10% 줄 때 이익이 감소하는 폭보다 적게 늘어난다.) 상방과 하방의 크기가 다른 것이 곧 프래질리티다.

#### 20장 「Time and Fragility」 — 린디 효과

20장은 "무엇이 오래 갈지"를 예측 없이 판정하는 방법을 다루고, 그 도구가 린디 효과다. 전제는 **소멸하는 것과 소멸하지 않는 것의 구분**이다.

> "A single car is perishable, but the automobile as a technology has survived about a century. ... The physical book is perishable—say, a specific copy of the Old Testament—but its contents are not."
>
> 자동차 한 대는 소멸하지만 기술로서의 자동차는 약 한 세기를 살아남았다. 물리적 책 한 권은 소멸하지만 그 내용은 그렇지 않다.

차이가 기대 수명의 방향을 뒤집는다. 젊은 사람과 늙은 사람을 보면 젊은 쪽이 더 오래 산다고 확신할 수 있지만, 소멸하지 않는 것은 반대다. "if the old is eighty and the young is ten, the elder is expected to live eight times as long as the younger one."(늙은 쪽이 80이고 젊은 쪽이 10이면, 늙은 쪽이 젊은 쪽보다 여덟 배 오래 살 것으로 기대된다.)

기술 선택에 쓰면 이렇게 된다. 20년을 버틴 동기화 기법은 작년에 나온 것보다 앞으로도 오래 갈 쪽이다. 이번 Station에서 `synchronized`와 `ReentrantLock`과 CAS를 먼저 익히는 것이 특정 분산 락 라이브러리를 먼저 익히는 것보다 유효 기간이 긴 투자라는 근거가 여기다.

#### 21장 「Medicine, Convexity, and Opacity」 — 개입의 정당성은 치료법이 아니라 환자 상태가 정한다

7장이 이아트로제닉스를 소개했다면 21장이 그것을 **결정 규칙**으로 만든다. 두 원칙이 있다.

제1원칙은 증거의 비대칭이다. "we do not need evidence of harm to claim that a drug or an unnatural via positiva procedure is dangerous."(어떤 약이나 자연스럽지 않은 더하기식 시술이 위험하다고 주장하기 위해 피해의 증거가 필요하지는 않다.) 근거는 칠면조 문제와 같다 — 피해는 좁게 정의된 과거가 아니라 미래에 있다.

제2원칙이 이 장의 핵심이고, 비선형성이다.

> "Second principle of iatrogenics: it is not linear. We should not take risks with near-healthy people; but we should take a lot, a lot more risks with those deemed in danger."
>
> 이아트로제닉스의 제2원칙: 그것은 선형이 아니다. 거의 건강한 사람에게는 위험을 감수하지 말아야 한다. 그러나 위험하다고 판단된 사람에게는 훨씬, 훨씬 더 많은 위험을 감수해야 한다.

혈압 약의 숫자가 근거다. 고혈압이 경계선 수준일 때 약으로 이득을 볼 확률은 5.6%(열여덟 명 중 한 명)이고, "high"와 "severe" 구간에서는 26%와 72%로 올라간다. 그래서 결정적인 문장이 나온다.

> "But consider that the iatrogenics should be constant for all categories! In the very ill condition, the benefits are large relative to iatrogenics; in the borderline one, they are small."
>
> 그런데 이아트로제닉스는 모든 범주에서 **일정**하다는 점을 생각해 보라! 매우 아픈 상태에서는 이득이 이아트로제닉스에 비해 크다. 경계선 상태에서는 작다.

같은 절의 요약이 더 간결하다. "the iatrogenics is in the patient, not in the treatment. If the patient is close to death, all speculative treatments should be encouraged—no holds barred. Conversely, if the patient is near healthy, then Mother Nature should be the doctor."(이아트로제닉스는 치료법에 있는 게 아니라 환자에게 있다. 환자가 죽음에 가까우면 모든 투기적 치료를 장려해야 한다. 거리낄 것이 없다. 반대로 환자가 거의 건강하면 자연이 의사가 되어야 한다.)

이 구조가 『자바 병렬 프로그래밍』 15.3.2절의 벤치마크 결론과 정확히 같다. **락의 비용은 경쟁 수준과 거의 무관하게 치러지지만, 락의 이득은 경쟁이 심해질수록 커진다.** 그래서 경쟁이 낮은 자원에 거는 비관적 락은 이득 없이 비용만 남는다. 탈레브의 언어로는 "거의 건강한 환자에게 투약하는 것"이다.

#### 23장 「Skin in the Game」 — 숨은 위험은 기초에 있다

23장은 상방을 가져가는 사람과 하방을 받는 사람이 다를 때 무슨 일이 생기는지를 다루고, 해법으로 함무라비 법전을 든다.

> "If a builder builds a house and the house collapses and causes the death of the owner of the house—the builder shall be put to death."
>
> 건축가가 집을 지었고 그 집이 무너져 집주인이 죽으면, 그 건축가를 사형에 처한다.

이 조항을 "최고의 위험 관리 규칙"이라 부르는 이유가 중요하다.

> "the builder knows more, a lot more, than any safety inspector, particularly about what lies hidden in the foundations—making it the best risk management rule ever, as the foundation, with delayed collapse, is the best place to hide risk."
>
> 건축가는 어떤 안전 검사관보다 더, 훨씬 더 많이 안다. 특히 기초에 무엇이 숨어 있는지를. 그래서 이것이 역사상 최고의 위험 관리 규칙이 된다. 붕괴가 지연되는 기초야말로 위험을 숨기기에 가장 좋은 자리이기 때문이다.

목적은 소급 처벌이 아니라 **사전 억지**다. "the object here is not to punish retrospectively, but to save lives by providing up-front disincentive."

동시성 결함이 이 "기초"의 정의에 그대로 들어맞는다. 재현이 비결정적이고 붕괴가 지연되며, 짠 사람이 격리 수준과 복합 동작의 경계를 가장 잘 안다. 리뷰어나 테스트가 아니라 **짠 사람이 재현 테스트를 함께 내야 하는** 근거가 여기다. 과제가 코드와 재현 테스트를 같은 레포에 요구하는 것도 같은 구조다.

---

### 읽기 범위 정리 — [기술] 『자바 동시성 프로그래밍』

**먼저 책 확인.** 과제의 저자 표기(브라이언 고에츠 외)로 보면 이 책은 Brian Goetz 외 5인의 *Java Concurrency in Practice*(Addison-Wesley, 2006)다. 국내 번역서 제목은 『**자바 병렬 프로그래밍**』(에이콘, 2008, 강철구 역)이고, 『자바 동시성 프로그래밍』이라는 제목의 다른 국내서가 따로 있으니 저자명으로 확인해서 구해야 한다. 아래 인용과 쪽수는 원서 기준이다.

#### 2장 — 동시성 버그의 메커니즘

과제가 "Read-Modify-Write를 본인 손으로 재현하라"고 한 대목의 근거가 2.2절이다. `++count`는 구문이 짧아 한 동작처럼 보이지만 세 개의 별개 연산이다.

> "it is shorthand for a sequence of three discrete operations: fetch the current value, add one to it, and write the new value back. This is an example of a read-modify-write operation, in which the resulting state is derived from the previous state." (2.2, p.13)
>
> 그것은 세 개의 별개 연산을 줄여 쓴 것이다 — 현재 값을 읽고, 1을 더하고, 새 값을 다시 쓴다. 이것이 read-modify-write 연산의 예이며, 결과 상태가 이전 상태에서 파생된다.

2.2.1절이 race condition을 정의한다. "A race condition occurs when the correctness of a computation depends on the relative timing or interleaving of multiple threads by the runtime; in other words, when getting the right answer relies on lucky timing."(계산의 정확성이 런타임의 상대적 타이밍이나 여러 스레드의 끼어들기 순서에 의존할 때 race condition이 발생한다. 다시 말해 정답을 얻는 것이 운 좋은 타이밍에 달려 있을 때다.) 가장 흔한 유형은 **check-then-act** — 낡을 수 있는 관찰로 다음 행동을 결정하는 것이고, 스타벅스 두 지점을 왕복하며 친구를 찾는 비유로 설명한다. 재고 차감은 check-then-act(재고가 있나 확인 → 차감)와 read-modify-write(이전 재고에서 1을 뺀다)가 겹친 전형이다.

**이 절에 과제의 핵심 경고가 있다.**

> "Like most concurrency errors, race conditions don't always result in failure: some unlucky timing is also required." (2.2.2, p.16)
>
> 대부분의 동시성 오류처럼 race condition이 늘 실패로 이어지는 것은 아니다. 운 나쁜 타이밍도 함께 있어야 한다.

2.2.3절은 check-then-act와 read-modify-write를 묶어 **복합 동작(compound action)** 이라 부르고, 스레드 안전을 위해 이것들이 항상 원자적이어야 한다고 정리한다. 3장의 결론은 "Locking can guarantee both visibility and atomicity; volatile variables can only guarantee visibility."(락은 가시성과 원자성을 모두 보장할 수 있다. volatile 변수는 가시성만 보장한다.)

#### 11장 — 락의 비용

11.3.3절이 경쟁 상태의 락이 왜 비싼지 설명한다. 경쟁이 없는 동기화는 JVM 안에서 처리되지만, 경쟁이 생기면 OS가 개입하고 진 스레드는 블록된다. 그 결과가 **컨텍스트 스위치 두 번**이다 — 블록된 스레드는 타임 퀀텀이 끝나기 전에 쫓겨나고, 나중에 자원이 풀린 뒤 다시 들어온다. 락을 쥔 쪽에도 비용이 있다. 해제할 때 OS에 블록된 스레드를 깨워 달라고 요청해야 한다.

11.4절의 명제가 이 장의 결론이다.

> "The principal threat to scalability in concurrent applications is the exclusive resource lock." (11.4, p.144)
>
> 동시성 애플리케이션에서 확장성을 위협하는 주된 요인은 배타적 자원 락이다.

경쟁 가능성을 정하는 두 요인은 **락을 얼마나 자주 요청하는가**와 **한 번 잡으면 얼마나 오래 쥐는가**이며, 이 둘의 곱이 작으면 경쟁은 확장성 장애가 되지 않는다(Little의 법칙의 계). 그래서 경쟁을 줄이는 방법도 셋이다 — 보유 시간을 줄이기, 요청 빈도를 줄이기, 배타 락을 더 많은 동시성을 허용하는 조정 수단으로 바꾸기.

11.4.1절의 산수가 과제에 바로 쓸 수 있다. 한 연산이 2ms 동안 락을 쥐고 모든 연산이 그 락을 필요로 하면, 프로세서가 몇 개든 처리량은 초당 500건을 넘을 수 없다. 1ms로 줄이면 1000건이 된다. 같은 절의 `AttributeStore` 예시는 정확히 과도한 락의 사례다 — 메서드 전체가 synchronized인데, 실제로 락이 필요한 부분은 `Map.get` 호출뿐이다.

#### 15장 — 비관적 락과 낙관적 락

15.1절 「Disadvantages of Locking」이 비관적 락의 비용을 열거한다. 여러 스레드가 동시에 락을 요청하면 JVM이 OS의 도움을 받고, 어느 불운한 스레드는 중단된 뒤 나중에 재개된다. 재개될 때는 다른 스레드들이 스케줄링 퀀텀을 다 쓰기를 기다려야 할 수도 있다. 그리고 **우선순위 역전** — 락을 쥔 저우선순위 스레드가 페이지 폴트로 지연되면, 그 락이 필요한 고우선순위 스레드는 실질적으로 저우선순위로 강등된다. 락을 쥔 스레드가 영구히 블록되면(무한 루프, 교착상태, 라이브락) 그 락을 기다리는 어떤 스레드도 영원히 진행하지 못한다.

15.2절이 두 기법을 가르는 문단이다. **제출물 2에 인용할 대목이 여기다.**

> "Exclusive locking is a pessimistic technique — it assumes the worst (if you don't lock your door, gremlins will come in and rearrange your stuff) and doesn't proceed until you can guarantee, by acquiring the appropriate locks, that other threads will not interfere.
> For fine-grained operations, there is an alternate approach that is often more efficient — the optimistic approach, whereby you proceed with an update, hopeful that you can complete it without interference. This approach relies on collision detection to determine if there has been interference from other parties during the update, in which case the operation fails and can be retried (or not)." (15.2, p.196)
>
> 배타적 락은 비관적 기법이다 — 최악을 가정하고(문을 잠그지 않으면 도깨비가 들어와 물건을 어질러 놓을 것이다), 적절한 락을 획득해 다른 스레드가 끼어들지 않을 것을 보장할 수 있을 때까지 진행하지 않는다.
> 세밀한 연산에는 종종 더 효율적인 대안이 있다 — 낙관적 접근이다. 방해 없이 완료할 수 있기를 바라며 그냥 업데이트를 진행한다. 이 접근은 업데이트 중에 다른 쪽의 방해가 있었는지 판정하기 위해 **충돌 감지**에 의존하고, 방해가 있었다면 연산은 실패하며 재시도할 수 있다(또는 하지 않는다).

낙관적 접근은 "허락보다 용서를 받기가 쉽다"는 속담과 같고, 여기서 "쉽다"는 "더 효율적이다"라는 뜻이라고 책은 덧붙인다.

15.2.1절은 CAS를 정의한다. 피연산자는 셋이다 — 대상 메모리 위치 V, 기대하는 이전 값 A, 새 값 B. V의 값이 A와 일치할 때만 V를 B로 원자적으로 바꾸고, 아니면 아무것도 하지 않는다. 책의 요약이 좋다. "CAS means 'I think V should have the value A; if it does, put B there, otherwise don't change it but tell me I was wrong.'"(CAS는 "V가 A라는 값을 갖고 있을 것 같다. 그렇다면 거기에 B를 넣고, 아니면 바꾸지 말고 내가 틀렸다고 알려 달라"는 뜻이다.) 그리고 "CAS is an optimistic technique — it proceeds with the update in the hope of success, and can detect failure if another thread has updated the variable since it was last examined."(CAS는 낙관적 기법이다. 성공을 바라며 업데이트를 진행하고, 마지막으로 값을 확인한 이후 다른 스레드가 그 변수를 바꿨다면 실패를 감지할 수 있다.)

경쟁에서 진 쪽의 처지가 락과 결정적으로 다르다. "the losers are not punished by suspension, as they could be if they failed to acquire a lock; instead, they are told that they didn't win the race this time but can try again."(패자는 락 획득에 실패했을 때처럼 중단으로 처벌받지 않는다. 대신 이번에는 경쟁에서 이기지 못했지만 다시 시도할 수 있다는 말을 듣는다.) 그래서 락의 활성성 위험 대부분이 사라지지만, 드문 경우 **라이브락** 위험이 생긴다.

15.2.2절이 낙관적 쪽의 비용을 한 문장으로 정리한다.

> "The primary disadvantage of CAS is that it forces the caller to deal with contention (by retrying, backing off, or giving up), whereas locks deal with contention automatically by blocking until the lock is available." (15.2.2, p.198)
>
> CAS의 주된 단점은 경쟁 처리를 호출자에게 떠넘긴다는 것이다(재시도하거나, 물러나거나, 포기하거나). 반면 락은 락이 사용 가능해질 때까지 블록하면서 경쟁을 자동으로 처리한다.

같은 절의 각주는 더 솔직하다. "Actually, the biggest disadvantage of CAS is the difficulty of constructing the surrounding algorithms correctly."(실은 CAS의 가장 큰 단점은 그것을 둘러싼 알고리즘을 올바르게 짜기가 어렵다는 점이다.)

15.3.2절의 벤치마크가 판정 기준을 준다. `ReentrantLock`과 `AtomicInteger`로 각각 구현한 난수 생성기를 비교했고, 결과는 경쟁 수준에 따라 뒤집힌다.

> "With low to moderate contention, atomics offer better scalability; with high contention, locks offer better contention avoidance." (15.3.2, p.201)
>
> 낮거나 중간 수준의 경쟁에서는 원자적 변수가 더 나은 확장성을 제공하고, 높은 경쟁에서는 락이 더 나은 경쟁 회피를 제공한다.

이유가 중요하다. 락은 경쟁에 **스레드를 중단시켜** 반응하므로 CPU 사용과 공유 메모리 버스의 동기화 트래픽을 줄인다. 반면 CAS 기반 알고리즘은 즉시 다시 시도하는 것으로 반응하는데, 이것이 보통은 옳지만 경쟁이 극심한 환경에서는 **경쟁을 더 만든다.** 다만 책은 그 벤치마크의 경쟁 수준이 비현실적으로 높다고 단서를 단다 — 락이나 원자적 변수를 두고 경쟁하는 일만 하는 프로그램은 없다. 실무의 전형적 경쟁 수준에서는 원자적 변수가 더 잘 확장된다.

비용 비교의 기준값도 적혀 있다. "the cost of the 'fast path' for uncontended lock acquisition and release on most processors is approximately twice the cost of a CAS."(대부분의 프로세서에서 경쟁 없는 락 획득과 해제의 "빠른 경로" 비용은 대략 CAS 비용의 두 배다.) 즉 락을 쓴다고 CAS가 사라지는 게 아니라, 락이 CAS를 **보이지 않는 곳으로 옮기고 그 위에 비용을 더 얹는다.** "using locks moves the CAS out of sight but doesn't save any actual execution cost."

#### 10장·12장 — 재현 테스트의 근거

과제가 "동시 요청 재현 테스트"를 요구하는 이유가 12장 서두에 있다. "the major challenge in constructing tests for concurrent programs is that potential failures may be rare probabilistic occurrences rather than deterministic ones; tests that disclose such failures must be more extensive and run for longer than typical sequential tests."(동시성 프로그램의 테스트를 만들 때 가장 큰 어려움은 잠재적 실패가 결정론적이 아니라 드문 확률적 사건일 수 있다는 점이다. 그런 실패를 드러내는 테스트는 통상의 순차 테스트보다 더 광범위하고 더 오래 돌아야 한다.)

12.1.6절 「Generating More Interleavings」는 확률을 올리는 방법을 준다 — 활성 스레드보다 프로세서 수가 적은 환경이 더 많은 끼어들기를 만들고, 공유 상태를 건드리는 연산 사이에 `Thread.yield()`(또는 짧지만 0이 아닌 sleep)를 끼우면 타이밍에 민감한 버그를 활성화할 수 있다. 책의 표현으로 "testing for concurrency errors is a numbers game"(동시성 오류 테스트는 숫자 게임이다).

10장의 각주가 안티프래질과 정확히 겹치는 역설을 지적한다.

> "Ironically, holding locks for short periods of time, as you are supposed to do to reduce lock contention, increases the likelihood that testing will not disclose latent deadlock risks." (10장 각주, p.131)
>
> 역설적으로, 락 경쟁을 줄이기 위해 응당 해야 하는 일인 "락을 짧게 쥐기"는 테스트가 잠재적 교착상태 위험을 드러내지 **못할** 가능성을 높인다.

---

### 읽기 범위 정리 — 『자바 동시성 프로그래밍』 중·후반부

앞에서 정리한 2·11·15장이 "무엇이 깨지고 왜 비싼가"를 다룬다면, 아래 장들은 **락을 직접 설계할 때 필요한 것**을 준다. 과제가 "락 라이브러리를 가져다 쓰는 것이 아니라 처음부터 설계한다"고 한 부분의 실제 근거다.

#### 2.4~2.5절 — 락의 경계는 불변식이 정한다

락을 어디까지 걸어야 하는지는 취향이 아니라 규칙으로 정해진다. 2.4절의 두 문장이 그 규칙이다.

> "For each mutable state variable that may be accessed by more than one thread, all accesses to that variable must be performed with the same lock held. In this case, we say that the variable is guarded by that lock." (2.4)
>
> 둘 이상의 스레드가 접근할 수 있는 각 가변 상태 변수에 대해, 그 변수에 대한 모든 접근은 **같은 락을 쥔 상태에서** 수행되어야 한다. 이 경우 그 변수는 그 락에 의해 보호된다고 말한다.

> "For every invariant that involves more than one variable, all the variables involved in that invariant must be guarded by the same lock." (2.4, p.20)
>
> 둘 이상의 변수가 관여하는 모든 불변식에 대해, 그 불변식에 관여하는 모든 변수는 **같은 락**에 의해 보호되어야 한다.

두 번째가 실무에서 더 자주 깨진다. 재고와 주문 이력을 함께 맞춰야 한다면 둘을 각각 다른 락으로 보호하는 것은 보호가 아니다. 두 변수를 한 원자적 연산으로 묶어야 불변식이 유지된다.

**그리고 "전부 synchronized"가 왜 답이 아닌지가 바로 이어진다.**

> "If synchronization is the cure for race conditions, why not just declare every method synchronized? It turns out that such indiscriminate application of synchronized might be either too much or too little synchronization." (2.4, p.20)
>
> 동기화가 race condition의 치료제라면, 왜 그냥 모든 메서드를 synchronized로 선언하지 않는가? 그런 무분별한 synchronized 적용은 동기화가 **너무 많거나 너무 적을** 수 있음이 드러난다.

"너무 적을 수도 있다"는 쪽이 중요하다. `Vector`처럼 모든 메서드를 동기화해도 `Vector`에 대한 복합 동작은 원자적이 되지 않는다. 즉 메서드 단위 동기화는 불변식의 경계와 무관하게 그어진 선이다.

**반론 — 그렇다고 성능을 위해 안전을 깎지는 않는다.** 2.5절이 균형점을 명시한다.

> "There is frequently a tension between simplicity and performance. When implementing a synchronization policy, resist the temptation to prematurely sacrifice simplicity (potentially compromising safety) for the sake of performance." (2.5, p.23)
>
> 단순성과 성능 사이에는 자주 긴장이 있다. 동기화 정책을 구현할 때, 성능을 위해 단순성을(그래서 잠재적으로 안전성을) 성급히 희생하려는 유혹에 저항하라.

같은 절은 경쟁하는 설계 압력을 safety, simplicity, performance 셋으로 열거하면서 safety에만 "which must not be compromised"(타협해서는 안 되는)라는 단서를 붙인다. 대신 범위를 좁히라고 한다 — "Avoid holding locks during lengthy computations or operations at risk of not completing quickly such as network or console I/O."(긴 계산이나 네트워크·콘솔 I/O처럼 빨리 끝나지 않을 위험이 있는 연산 중에는 락을 쥐지 말라.)

#### 3장 「Sharing Objects」 — 가시성: 락이 보장하는 나머지 절반

3장의 도입이 흔한 오해를 바로잡는다. 2장을 읽고 "락은 임계 영역을 긋는 것"이라고 이해하면 절반만 가져간 것이다.

> "it is a common misconception that synchronized is only about atomicity or demarcating 'critical sections'. Synchronization also has another significant, and subtle, aspect: memory visibility." (3장 도입)
>
> synchronized가 원자성이나 "임계 영역" 구획에 관한 것일 뿐이라는 생각은 흔한 오해다. 동기화에는 또 하나의 중요하고 미묘한 측면이 있다 — **메모리 가시성**이다.

3.1절의 `NoVisibility` 예시가 그 결과를 보여 준다. 메인 스레드가 `number = 42`를 쓰고 `ready = true`를 쓰면, 리더 스레드는 `ready`가 참이 될 때까지 돌다가 `number`를 출력한다. 당연히 42가 나올 것 같지만 책의 말은 이렇다 — "it is in fact possible that it will print zero, or never terminate at all!"(실제로 0을 출력할 수도 있고, 아예 종료되지 않을 수도 있다!) 동기화가 없으면 컴파일러는 연산을 재배치하고 값을 레지스터에 캐시할 수 있으며, CPU는 프로세서별 캐시에 값을 둘 수 있다.

**이 장이 과제에 주는 경고는 "추론으로 때우지 말라"는 것이다.**

> "Attempts to reason about the order in which memory actions 'must' happen in insufficiently synchronized multithreaded programs will almost certainly be incorrect. ... Reasoning about insufficiently synchronized concurrent programs is prohibitively difficult." (3.1)
>
> 충분히 동기화되지 않은 멀티스레드 프로그램에서 메모리 동작이 "반드시" 일어날 순서를 추론하려는 시도는 거의 확실히 틀린다. 충분히 동기화되지 않은 동시성 프로그램에 대해 추론하는 일은 감당할 수 없을 만큼 어렵다.

3.1.1절의 **stale data**(낡은 데이터)에 과제와 직결되는 두 가지가 있다. 첫째, 낡음은 전부 아니면 전무가 아니다 — "a thread can see an up-to-date value of one variable but a stale value of another variable that was written first."(한 스레드가 어떤 변수는 최신 값으로 보면서, 먼저 쓰인 다른 변수는 낡은 값으로 볼 수 있다.) 둘째, 이 절의 각주가 **DB 격리 수준으로 바로 연결된다.**

> "Reading data without synchronization is analogous to using the READ_UNCOMMITTED isolation level in a database, where you are willing to trade accuracy for performance. However, in the case of unsynchronized reads, you are trading away a greater degree of accuracy, since the visible value for a shared variable can be arbitrarily stale." (3.1.1 각주)
>
> 동기화 없이 데이터를 읽는 것은 DB에서 READ_UNCOMMITTED 격리 수준을 쓰는 것과 유사하다. 정확성을 성능과 맞바꾸는 것이다. 다만 동기화 없는 읽기에서는 더 큰 정도의 정확성을 내주게 된다. 공유 변수의 보이는 값이 **임의로 낡아** 있을 수 있기 때문이다.

3.1.2절에는 별도의 함정이 하나 있다. volatile이 아닌 `long`과 `double`은 JVM이 64비트 읽기·쓰기를 **두 번의 32비트 연산으로 처리해도 된다.** 그래서 동기화 없이 공유하면 한쪽의 상위 32비트와 다른 쪽의 하위 32비트가 섞인, 어느 스레드도 쓴 적 없는 값을 읽을 수 있다.

3.1.3절이 락과 가시성을 잇는다. 스레드 A가 synchronized 블록을 실행하고 이어서 스레드 B가 **같은 락**으로 보호되는 synchronized 블록에 들어가면, A가 락을 풀기 전에 보던 값들이 B가 락을 얻을 때 보이도록 보장된다. 그래서 2.4절의 "같은 락" 요구에 두 번째 이유가 생긴다.

> "Locking is not just about mutual exclusion; it is also about memory visibility." (3.1.3)
>
> 락은 상호 배제에 관한 것만이 아니다. 메모리 가시성에 관한 것이기도 하다.

#### 4.1절 — 스레드 안전 클래스의 설계 절차

"락을 처음부터 설계한다"는 과제 문구의 구체적 절차가 4.1절에 있다. 세 단계다.

> "The design process for a thread-safe class should include these three basic elements:
> • Identify the variables that form the object's state;
> • Identify the invariants that constrain the state variables;
> • Establish a policy for managing concurrent access to the object's state." (4.1)
>
> 스레드 안전 클래스의 설계 과정은 이 세 가지 기본 요소를 포함해야 한다 — 객체의 상태를 구성하는 변수를 식별한다. 그 상태 변수들을 제약하는 불변식을 식별한다. 객체 상태에 대한 동시 접근을 관리하는 **정책**을 세운다.

순서가 핵심이다. 락이 1단계가 아니라 3단계다. 그리고 캡슐화가 이 절차를 가능하게 만드는 전제라고 적는다 — "Encapsulation makes it possible to determine that a class is thread-safe without having to examine the entire program."(캡슐화는 전체 프로그램을 검사하지 않고도 어떤 클래스가 스레드 안전한지 판정할 수 있게 한다.) 상태가 public static 필드로 흩어져 있으면 그 판정 자체가 불가능해진다. 3.2절의 용어로 그런 상태는 의도한 범위를 **벗어난(escaped)** 것이고, 한번 벗어나면 다른 클래스나 스레드가 그것을 오용할 수 있다고 가정해야 한다.

#### 5.5절 — 재현 테스트의 도구: 래치와 배리어

과제가 "ExecutorService(또는 CountDownLatch)"를 지정한 이유가 5.5.1절과 12장에 함께 있다. 먼저 래치의 정의다.

> "A latch is a synchronizer that can delay the progress of threads until it reaches its terminal state. A latch acts as a gate: until the latch reaches the terminal state the gate is closed and no thread can pass, and in the terminal state the gate opens, allowing all threads to pass. Once the latch reaches the terminal state, it cannot change state again, so it remains open forever." (5.5.1)
>
> 래치는 자신이 종료 상태에 도달할 때까지 스레드들의 진행을 지연시킬 수 있는 동기화 도구다. 래치는 문처럼 작동한다. 종료 상태에 도달하기 전에는 문이 닫혀 어떤 스레드도 통과할 수 없고, 종료 상태가 되면 문이 열려 모든 스레드가 통과한다. 한번 종료 상태가 되면 다시 상태를 바꿀 수 없어 영원히 열린 채로 남는다.

`CountDownLatch`는 양수로 초기화된 카운터를 갖고, `countDown()`이 카운터를 줄이며 `await()`는 카운터가 0이 될 때까지 블록한다. 책의 `TestHarness`(Listing 5.11)가 **startGate 1개와 endGate N개**로 이것을 쓴다. 모든 워커가 `startGate.await()`에서 대기하고, 드라이버가 문을 열면 동시에 출발한다.

**왜 그냥 루프로 스레드를 시작하면 안 되는지는 12.1절이 설명한다.**

> "If your thread is short-running and you start a number of threads in a loop, the threads run sequentially rather than concurrently in the worst case. Even in the not-quite-worst case, the fact that the first thread has a head start on the others means that you may get fewer interleavings than expected." (12.1)
>
> 스레드가 짧게 도는 것이고 여러 개를 루프에서 시작하면, 최악의 경우 스레드들은 동시가 아니라 **순차로** 돈다. 최악에 가깝지 않은 경우에도 첫 스레드가 다른 스레드들보다 먼저 출발했다는 사실은 기대보다 **적은 끼어들기**를 얻게 된다는 뜻이다.

같은 절은 대안으로 `CyclicBarrier`(워커 수 + 1로 초기화해 드라이버까지 시작과 끝에서 대기)를 제시하고, 효과를 "This ensures that all threads are up and running before any start working"(어떤 스레드도 일을 시작하기 전에 모든 스레드가 떠서 돌고 있음을 보장한다)으로 적는다. 즉 **래치 없이 쓴 재현 테스트는 동시성을 재현하지 못한 채 초록불을 줄 수 있다.** 과제에서 "왜 그 방식을 골랐는지"를 묻는 질문의 답이 여기 있다.

5.5절의 다른 도구도 역할이 갈린다. `FutureTask`는 계산을 수행한 스레드에서 결과를 가져가는 스레드로 결과를 넘기는데, 그 전달이 **안전한 공개(safe publication)** 가 됨을 명세가 보장한다. `Semaphore`는 동시에 어떤 자원에 접근하는 활동의 수를 제한한다.

#### 13장 「Explicit Locks」 — 명시적 락이 더 주는 것과 더 위험해지는 것

`ReentrantLock`이 내재 락(`synchronized`)보다 주는 것은 네 가지다. 13.1절은 `Lock` 인터페이스가 **무조건·폴링·타임아웃·인터럽트 가능** 획득을 제공하고 락과 해제가 모두 명시적이라고 적는다. 가시성 의미는 같아야 한다 — "Lock implementations must provide the same memory-visibility semantics as intrinsic locks."

가장 실용적인 추가 기능은 `tryLock`이다.

> "With intrinsic locks, a deadlock is fatal - the only way to recover is to restart the application, and the only defense is to construct your program so that inconsistent lock ordering is impossible. Timed and polled locking offer another option: probabilistic deadlock avoidance." (13.1.1)
>
> 내재 락에서 교착상태는 치명적이다. 복구하는 유일한 방법은 애플리케이션을 재시작하는 것이고, 유일한 방어는 일관되지 않은 락 순서가 불가능하도록 프로그램을 구성하는 것이다. 타임아웃·폴링 락은 또 하나의 선택지를 준다 — **확률적 교착상태 회피**다.

구체적으로는 두 락을 `tryLock`으로 시도하고, 둘 다 못 얻으면 **쥔 것을 풀고 물러나 재시도**한다. 대기 시간에는 고정 성분과 무작위 성분을 함께 두어 라이브락 가능성을 줄인다. 시간 안에 못 얻으면 실패 상태를 반환해 연산이 우아하게 실패하게 한다. 15장의 낙관적 접근과 모양이 같다는 점이 눈에 띈다 — 실패를 감지하고 호출자가 결정한다.

**13.3절 공정성은 선착순 기능에 직접 걸리는 함정이다.** `ReentrantLock`은 비공정(기본값)과 공정 중에서 고를 수 있다. 공정 락은 요청한 순서대로 획득하고, 비공정 락은 **새치기(barging)** 를 허용한다.

> "fairness has a significant performance cost because of the overhead of suspending and resuming threads. ... The fairness penalty is nearly two orders of magnitude. Don't pay for fairness if you don't need it." (13.3)
>
> 공정성은 스레드를 중단하고 재개하는 오버헤드 때문에 상당한 성능 비용을 치른다. 공정성 대가는 거의 **100배**에 달한다. 필요하지 않다면 공정성에 돈을 쓰지 말라.

이유도 적혀 있다. 중단된 스레드가 재개되는 시점과 실제로 도는 시점 사이에 상당한 지연이 있어서, 그 사이 새로 들어온 스레드가 락을 얻어 쓰고 반납하는 것이 전체 처리량에 더 낫다. 선착순 쿠폰에서 "선착순"이 **락 획득 순서**를 뜻하는지 아니면 **요청 도착 순서로 결정된 결과**를 뜻하는지 구분해야 하는 근거가 이것이다. 후자라면 공정 락이 필요하지 않다.

**13.4절은 그래도 기본은 `synchronized`라고 못 박는다.**

> "ReentrantLock is an advanced tool for situations where intrinsic locking is not practical. Use it if you need its advanced features: timed, polled, or interruptible lock acquisition, fair queuing, or non-block-structured locking. Otherwise, prefer synchronized." (13.4)
>
> `ReentrantLock`은 내재 락이 실용적이지 않은 상황을 위한 고급 도구다. 타임아웃·폴링·인터럽트 가능 락 획득, 공정 큐, 블록 구조가 아닌 락 같은 고급 기능이 필요하면 쓰라. 그렇지 않으면 **synchronized를 선호하라.**

이유가 과제의 "AI가 짠 코드를 판정한다"는 주제와 겹친다. "if you forget to wrap the unlock call in a finally block, your code will probably appear to run properly, but you've created a time bomb"(`unlock` 호출을 `finally` 블록으로 감싸는 것을 잊으면, 코드는 아마 제대로 도는 것처럼 보이겠지만 시한폭탄을 만든 것이다). 그리고 스레드 덤프가 어느 호출 프레임이 어느 락을 쥐었는지 보여 주고 교착상태를 탐지하는 것은 내재 락의 이점이다(Java 6에서 `ReentrantLock`도 관리 인터페이스로 개선됨).

**13.5절 읽기-쓰기 락**은 배타 락이 필요 이상으로 동시성을 막는 경우를 겨냥한다. 상호 배제는 쓰기-쓰기와 쓰기-읽기 겹침을 막지만 **읽기-읽기 겹침까지** 막는다. `ReadWriteLock`은 여러 읽기 또는 하나의 쓰기를 허용한다. 다만 책은 조건을 분명히 단다 — "read-write locks can improve performance for frequently accessed read-mostly data structures on multiprocessor systems; under other conditions they perform slightly worse than exclusive locks due to their greater complexity."(읽기-쓰기 락은 멀티프로세서 시스템에서 자주 접근되는 읽기 위주 자료구조의 성능을 개선할 수 있다. 다른 조건에서는 복잡도가 더 커서 배타 락보다 약간 더 나쁘다.)

#### 14장 「Building Custom Synchronizers」 — 상태 의존 연산

14장은 "큐가 비어 있으면 꺼낼 수 없다"처럼 **상태 기반 선행 조건**을 가진 연산을 다룬다. 단일 스레드와 동시성의 차이가 여기서 갈린다.

> "In a single-threaded program, if a state-based precondition (like 'the connection pool is nonempty') does not hold when a method is called, it will never become true. ... But in a concurrent program, state-based conditions can change through the actions of other threads." (14.1)
>
> 단일 스레드 프로그램에서는 메서드 호출 시점에 상태 기반 선행 조건("커넥션 풀이 비어 있지 않다" 같은)이 성립하지 않으면 그것은 결코 참이 되지 않는다. 그러나 동시성 프로그램에서는 상태 기반 조건이 **다른 스레드의 행동을 통해** 바뀔 수 있다.

그래서 선택지가 "조건이 안 맞으면 실패한다"와 "조건이 참이 되기를 기다린다" 둘이 되고, 후자가 보통 더 편하고 오류가 적다고 적는다. 도구는 세 층이다 — 내재 조건 큐(`wait`/`notify`), 명시적 `Condition` 객체, 그리고 `AbstractQueuedSynchronizer`(AQS) 프레임워크.

설계 순서에 대한 권고가 분명하다. "The easiest way to construct a state-dependent class is usually to build on top of an existing state-dependent library class"(상태 의존 클래스를 만드는 가장 쉬운 방법은 보통 기존 상태 의존 라이브러리 클래스 위에 쌓는 것이다). 즉 `CountDownLatch`나 `Semaphore`로 되는 일을 AQS로 내려가서 짜지 않는다.

14.3절은 명시적 `Condition`이 필요한 지점을 짚는다. 내재 조건 큐는 **락 하나에 조건 큐 하나**뿐이어서, 여러 스레드가 서로 다른 조건 술어를 두고 같은 큐에서 기다리게 된다. 조건 술어가 둘 이상인 객체를 쓰려면 `Lock.newCondition()`으로 락 하나에 대기 집합 여럿을 만들어야 한다. AQS는 동기화 메서드가 "자기 동작이 대기 중인 스레드를 깨울 수 있었는지"를 반환값으로 알리게 강제해서, 상태 전이에서 통지를 빠뜨리기 어렵게 만든다.

#### 16장 「The Java Memory Model」 — 왜 락이 가시성을 보장하는가

16장은 3장이 "그렇다"고 말한 것의 근거를 준다. JMM은 읽기·쓰기, 모니터 락·해제, 스레드 시작·조인을 **동작(action)** 으로 보고 그 위에 **happens-before**라는 부분 순서를 정의한다.

> "To guarantee that the thread executing action B can see the results of action A ... there must be a happens-before relationship between A and B. In the absence of a happens-before ordering between two operations, the JVM is free to reorder them as it pleases." (16.1.3)
>
> 동작 B를 실행하는 스레드가 동작 A의 결과를 볼 수 있음을 보장하려면 A와 B 사이에 happens-before 관계가 있어야 한다. 두 연산 사이에 happens-before 순서가 없으면 JVM은 마음대로 그들을 재배치할 수 있다.

규칙 목록 중 이번 Station에 필요한 것은 넷이다. **프로그램 순서 규칙**(한 스레드 안의 각 동작은 그 뒤에 오는 동작보다 먼저 일어난다), **모니터 락 규칙**(어떤 모니터 락의 해제는 같은 모니터 락의 이후 획득보다 먼저 일어난다), **volatile 변수 규칙**(volatile 필드에 쓰기는 같은 필드의 이후 읽기보다 먼저 일어난다), **전이성**. 각주 둘이 중요하다 — 명시적 `Lock` 객체의 락·해제는 내재 락과 **같은** 메모리 의미를 가지며, 원자적 변수의 읽기·쓰기는 volatile과 **같은** 메모리 의미를 가진다. 15장의 CAS가 가시성까지 주는 근거가 이 각주다.

**data race의 정의도 여기 있다.** "A data race occurs when a variable is read by more than one thread, and written by at least one thread, but the reads and writes are not ordered by happens-before."(어떤 변수를 둘 이상의 스레드가 읽고 최소 하나가 쓰는데 그 읽기와 쓰기가 happens-before로 순서가 정해지지 않으면 data race가 발생한다.) 2.2.1절의 각주가 race condition과 data race가 다른 개념이라고 구분해 둔 것과 짝을 이룬다.

16.2.4절의 **이중 검사 락(DCL)** 은 과제의 "AI가 그럴듯한 코드를 내놓는다"는 문제의 교과서적 사례다. 책은 이것을 anti-pattern으로 분류하고 진짜 문제를 이렇게 짚는다.

> "The real problem with DCL is the assumption that the worst thing that can happen when reading a shared object reference without synchronization is to erroneously see a stale value (in this case, null) ... But the worst case is actually considerably worse - it is possible to see a current value of the reference but stale values for the object's state, meaning that the object could be seen to be in an invalid or incorrect state." (16.2.4)
>
> DCL의 진짜 문제는 동기화 없이 공유 객체 참조를 읽을 때 일어날 수 있는 최악이 낡은 값(여기서는 null)을 잘못 보는 것이라는 가정이다. 그러나 실제 최악은 상당히 더 나쁘다 — **참조는 최신 값으로 보면서 객체의 상태는 낡은 값으로** 볼 수 있다. 즉 객체가 유효하지 않거나 올바르지 않은 상태로 보일 수 있다.

Java 5.0 이후 `resource`를 volatile로 만들면 DCL이 동작하게 되었지만, 책의 결론은 쓰지 말라는 쪽이다. 그것을 낳은 동기(느린 비경쟁 동기화, 느린 JVM 시작)가 더 이상 유효하지 않으므로 최적화로서의 효용이 사라졌고, lazy initialization holder 관용구가 같은 이득을 주면서 이해하기 쉽다.

16.3절 **초기화 안전성**이 "공유하지 않거나 불변으로 만든다"는 전략의 근거를 완성한다. 제대로 구성된 불변 객체는 **어떻게 공개되든, 심지어 data race로 공개되어도** 동기화 없이 스레드 간에 안전하게 공유된다. 보장 범위는 `final` 필드다 — 생성자가 설정한 `final` 필드의 값, 그리고 그 필드를 통해 도달할 수 있는 변수들(`final` 배열의 원소, `final` 필드가 참조하는 `HashMap`의 내용)까지 다른 스레드에 보이도록 보장된다. 생성자가 완료되면 그 쓰기들이 "동결(frozen)"되고, 그 객체 참조를 얻은 스레드는 최소한 동결된 값만큼 최신인 값을 보게 된다.

---

### 제출물 보강 — 중·후반부에서 더할 것

**제출물 1** — 아래 본문에 반영했다. 충격 구절을 억제된 변동성(5장) · 자갈 천 개와 돌 하나(18장) · "취약성은 측정할 수 있고 위험은 측정할 수 없다"(프롤로그) 셋으로 잡고, 번호 없는 마무리 절로 닫았다. 칠면조는 5장 문단의 근거로, 19장의 탐지 휴리스틱은 18장 문단의 판정 절차로 흡수했다. 세 문단이 역할을 나눠 갖는다 — 왜 스트레스를 넣는가, 어떤 모양으로 넣어 무엇을 재는가, 테스트가 끝낼 수 없는 것을 무엇으로 보장하는가. 마무리는 주체를 바꾼다. 코드는 스스로 적응하지 않으므로 안티프래질해질 수 있는 쪽은 코드가 아니라 고치는 루프이고, 호르메시스(1장)와 개별·전체의 프래질리티(4장)를 그 근거로 쓴다. 7장의 이아트로제닉스와 18장의 규모 논증은 독립 문단에서 빼고 세 번째 문단의 한정 한 문장으로 내렸다 — 락의 비용은 제출물 2가 전담하므로 여기서 숫자를 또 쓰면 두 제출물이 겹친다. 21장의 제2원칙과 11장의 바벨 전략도 같은 이유로 F-3 락 설계로 미룬다.

**제출물 2**에 3·13·16장을 넣으면 "보장하는 것"의 목록이 정확해진다. 지금 초안은 비관적 락의 가시성 보장을 3장의 한 문장으로 인용했는데, 16.1.3절의 모니터 락 규칙과 그 각주(명시적 `Lock`과 원자적 변수가 같은 메모리 의미를 갖는다)를 쓰면 **CAS가 왜 가시성까지 주는지**를 근거로 적을 수 있다. 13.3절의 공정성 대가(약 100배)는 선착순 기능을 다룰 때 반드시 들어가야 하는 숫자다. 13.1.1절의 `tryLock` 교착상태 회피는 비관적 락도 "실패를 호출자에게 돌려줄 수 있다"는 점을 보여 주므로, 두 기법의 경계가 단순 이분법이 아니라는 한정을 더할 수 있다.

---

### 제출물 1 — [안티프래질] 충격 구절 세 가지와 관점의 변화

**(1) "인공적으로 억제된 변동성의 문제는 시스템이 극도로 프래질해진다는 것만이 아니다. 동시에 눈에 보이는 위험이 사라진다는 것이다. 변동성은 정보다."**

단일 요청 테스트는 안전한 환경이 아니라 변동성을 손으로 제거한 환경이다. 요청을 하나씩 순서대로 보내면 다른 요청이 끼어들 자리가 없어지고, 그 상태의 초록불은 코드가 안전하다는 정보가 아니라 **내가 정보를 차단했다는 사실**을 알려 준다. 책은 이것을 산불로 설명한다. 작은 산불은 가장 잘 타는 연료를 주기적으로 태워 없애는데, 안전하자고 그것까지 막으면 연료가 쌓여 큰 산불이 훨씬 나빠진다.

같은 착각에 책이 붙인 이름이 **"증거의 부재를 부재의 증거로 착각하는 것"**이다. 1000일 동안 먹이를 받은 칠면조의 확신은 믿음이 깨지는 바로 그 순간에 최대이고, 그때 칠면조의 삶은 "매우 조용하고 안심이 되도록 예측 가능한" 상태다. 나도 "운영에서 한 번도 안 터졌다"를 안전의 근거로 써 왔다. 『자바 병렬 프로그래밍』이 같은 말을 더 정확하게 한다 — race condition(여러 스레드의 실행 순서에 따라 결과가 달라지는 결함)은 늘 실패로 이어지지 않고 운 나쁜 타이밍이 함께 와야 터진다. 그러니 **통과한 이력은 코드가 옳다는 증거와 타이밍이 운 좋았다는 증거를 구분해 주지 않는다.** 재현 테스트의 위치가 여기서 바뀌었다. 전에는 검증 절차 중 하나였고, 이제는 동시성에 대해 내가 아는 것의 유일한 출처다. 버그를 찾는 작업이 아니라 내가 없애 둔 변동성을 코드에 되돌려 주는 작업이다.

**(2) "무언가가 아직 멀쩡하다는 조건 아래에서, 그것은 자갈 천 개보다 돌 하나에 더 상한다. 즉 작은 충격들의 누적 효과보다 드물고 큰 단일 사건에 더 상한다."**

피해가 강도에 비선형이면 **평균 지표는 거짓말을 한다.** 책의 자동차 예(시속 50마일로 한 번 박는 피해가 시속 5마일로 열 번 박는 피해의 열 배보다 크다)는 읽으면 당연한데, 당연해서 그냥 넘긴 대가가 내 대시보드에 있었다. 나는 평균 응답 시간과 분당 처리량을 보고 "안정적"이라고 판단해 왔다. 그 두 숫자는 충격의 **강도**를 평균으로 녹여 없애기 때문에, 비선형이 일어나는 자리를 정확히 가린다.

동시성에서 그 비선형이 어디서 나오는지는 겹침의 산수로 보인다. 서로 끼어들 수 있는 요청 쌍의 수는 동시 요청 수에 비례하지 않는다 — 동시 10이면 한 회차에 45쌍, 동시 100이면 4950쌍이다(책의 주장이 아니라 내 계산이다). 총 1000건을 보내도 겹칠 기회가 열 배 넘게 차이나고, 깨졌을 때 lost update(한쪽의 갱신이 조용히 덮여 사라지는 것)가 몇 건 겹치는지도 같이 커진다. 그러니 동시 10 요청을 열 번 돌린 것과 동시 100 요청을 한 번 돌린 것은 총 요청 수가 같아도 같은 테스트가 아니다.

그래서 **재는 방식을 두 군데서 고쳐야 한다**는 결론으로 옮겼다. 첫째, 부하 테스트의 판정 기준이다. 동시 요청을 50 → 100 → 150으로 올리며 p99 지연(요청 100건 중 99건이 그 안에 끝나는 시간)의 **증가분이 커지는지**를 본다. 증가분이 일정하면 선형이고, 커지면 그 자원이 프래질하다. 둘째, 운영 모니터링이다. 평균과 처리량만 걸어 두면 비선형은 임계를 넘는 순간에 처음 보이고, 그때는 이미 장애다. 강도에 따라 변하는 양을 지표로 올려야 한다 — 평균이 아닌 p99와 p999, 락 대기 시간, 트랜잭션 롤백과 재시도 비율, 커넥션 풀 대기 건수. 지금 내 모니터링에는 앞의 둘이 없다.

다만 비유를 그대로 믿으면 반대쪽으로 위험하다. 책의 명제는 **"아직 멀쩡하면서 비선형 효과에 노출된"** 것에만 적용된다고 조건이 달려 있다. 커넥션 누수, 락 해제 누락, 느리게 자라는 메모리처럼 누적으로 터지는 결함은 그 조건 밖이고, 그쪽은 자갈을 오래 던져야 보인다. 돌 하나로 바꾸는 것이 아니라 둘을 따로 돌리고 각각 다른 지표를 보는 게 맞다. 그리고 가속을 재는 것으로도 끝나지 않는다 — 동시 150까지 통과한 것이 151에서도 통과한다는 뜻은 아니기 때문이다.

**(3) "무언가가 프래질한지 알아내는 것은 그것을 해칠 사건의 발생을 예측하는 것보다 훨씬 쉽다. 취약성은 측정할 수 있다. 위험은 측정할 수 없다."**

동시성 설계는 끼어들기의 순서를 예측하는 일이 아니라, **어떤 순서가 와도 깨지지 않는 구조를 만드는 일**이다. 이 구절이 (1)과 (2)에 한정을 걸어서 가장 많이 흔들었다. 재현 테스트는 가능한 인터리빙(스레드들의 실행 순서 조합) 중 일부만 돌려 본다. 그래서 테스트는 결함이 있다는 것을 보일 수 있을 뿐, 없다는 것을 보이지 못한다. 나는 그동안 깨뜨렸다가 고쳤으니 끝이라고 생각해 왔는데, 그건 (1)의 착각을 방향만 바꿔 되풀이한 것이다. 조용해서 안전하다고 믿은 것과 한 번 깨뜨려 고쳤으니 안전하다고 믿은 것은 둘 다 테스트를 **부재의 증거**로 쓴 것이다.

예측을 포기해도 손에 남는 것이 있다는 게 이 구절의 요점이다. 책의 근거는 취약성이 **현재의 속성**이라는 점이다. 어떤 충격이 올지는 몰라도, 그 충격이 오면 이것이 저것보다 더 프래질하다는 판정은 지금 할 수 있다. 선착순 쿠폰에 동시 요청이 몇 건 들어올지, 어느 순간에 끼어들기가 생길지는 예측할 수 없다. 그러나 재고 차감이 읽고 고쳐 쓰는(read-modify-write) 형태인지는 **코드를 보면 지금 알 수 있고**, 그 격리 수준에서 lost update가 가능한지도 지금 판정할 수 있다. 과제가 요구하는 "AI가 짠 동시성 처리를 판정하는 능력"이 이것이라고 이해했다. 그 코드가 실제로 돌지를 확률로 추정하려 하면 답이 없지만, 락이 보호해야 할 동작 **전체**를 덮는지는 측정 가능한 질문이다.

그래서 질문의 순서가 바뀌었다. 전에는 "락을 걸었는가"를 물었다. 이제는 셋을 차례로 묻는다. 첫째, 이 상태를 아예 공유하지 않을 수 있는가. 둘째, 공유해야 한다면 그 동작이 확인하고 행동하는(check-then-act) 형태인가 읽고 고쳐 쓰는 형태인가. 셋째, 그 성격과 경쟁 수준에 비관적 락과 낙관적 락 중 어느 쪽이 맞는가. 락은 마지막 질문의 답일 뿐이다. 다만 반대쪽으로 넘어가지 않도록 한정을 하나 달아 둔다 — 전부 잠그면 그 구간이 직렬화되어 전체 처리량의 천장이 되므로, 잠글 범위는 불변식이 요구하는 만큼으로 **작게 쪼갠다.** 책도 규모가 커질수록 프래질해진다고 적는데, 락에서는 굵은 락 하나가 모든 요청을 줄 세우는 것이 그 큰 규모다.

**마무리 — 강해지는 것은 코드가 아니다**

세 구절을 적고 나서 걸린 것이 있다. 책에서 충격을 받아 강해지는 것은 살아 있는 것이고, 코드는 스스로 적응하지 않는다. 운동으로 근육이 커지는 식의 안티프래질리티를 코드에 그대로 옮기면 틀린 말이 된다. 동시 요청을 백 번 받은 코드가 저절로 단단해지지는 않는다.

그래서 주체를 바꿔야 맞다. 프래질한 쪽은 코드이고, 안티프래질해질 수 있는 쪽은 **그 코드를 고치는 루프**다. 책도 개별 요소의 프래질리티가 전체의 안티프래질리티를 만든다고 적는다 — 세포가 죽어야 장기가 건강하고, 부실기업이 망해야 경제가 진화한다. 동시성에 옮기면 요청 하나를 지키려 들지 않는 쪽이 전체를 살린다. 낙관적 락이 경쟁에서 진 요청에 실패를 돌려주는 것이 정확히 그 형태다. 개별 요청은 프래질하게 두고, 그 실패가 빨리 보이게 만들어 고치는 루프가 강해진다.

그러면 조건이 하나 붙는다. 저자가 **"호르메시스가 정상이고, 그것의 부재가 우리를 해친다"**고 쓴 것은 자극을 한 번 받는 상태가 아니라 계속 받는 상태를 말한다. 재현 테스트를 돌려 버그를 잡고 지우면 자극은 한 번으로 끝나므로, 고친 뒤에도 CI에 남겨 계속 돌려야 한다. 여기까지는 회귀 테스트의 상식이다.

내가 조심해야 할 지점은 그다음이다. 동시성 재현 테스트는 비결정적이라 같은 코드에서도 가끔 통과하고 가끔 깨진다. 그렇게 깨졌을 때 "그 테스트가 원래 불안정하다"며 재실행으로 넘기거나 비활성화하면, **자극을 차단한 것**이 된다. 테스트는 그대로 있고 파이프라인은 초록불인데 정보는 사라진다. (1)에서 적은 억제된 변동성이 이번에는 내 손으로 파이프라인에 들어오는 것이고, 조용한 CI가 안전의 증거로 다시 쓰이기 시작한다. 그래서 점검할 것은 "지금 초록불인가"가 아니라 "이 테스트가 최근에 깨진 적이 있고 그때 원인을 찾았는가"다.

> **[본인 경험으로 교체할 부분]** 지시문이 요구하는 "이전 환경에서 겪은 동시성 이슈와 그 원인·대처"가 들어갈 자리는 (1)과 (3)이다. (1)의 "운영에서 한 번도 안 터졌다를 안전의 근거로 써 왔다"와 (3)의 "깨뜨렸다가 고쳤으니 끝이라고 생각해 왔다"에 실제 사례를 넣으면 그 항목이 같이 닫힌다. (2)는 과거 사례가 아니라 앞으로의 테스트 설계를 맡은 자리여서 사례를 넣으면 역할이 흐려진다.

---

### 제출물 2 — [기술서] 비관적 락과 낙관적 락

**인용** (『자바 병렬 프로그래밍』 15.2절, 원서 p.196)

> "Exclusive locking is a pessimistic technique — it assumes the worst (if you don't lock your door, gremlins will come in and rearrange your stuff) and doesn't proceed until you can guarantee, by acquiring the appropriate locks, that other threads will not interfere. For fine-grained operations, there is an alternate approach that is often more efficient — the optimistic approach, whereby you proceed with an update, hopeful that you can complete it without interference. This approach relies on collision detection to determine if there has been interference from other parties during the update, in which case the operation fails and can be retried (or not)."
>
> 배타적 락은 비관적 기법이다 — 최악을 가정하고(문을 잠그지 않으면 도깨비가 들어와 물건을 어질러 놓을 것이다), 적절한 락을 획득해 다른 스레드가 끼어들지 않을 것을 보장할 수 있을 때까지 진행하지 않는다. 세밀한 연산에는 종종 더 효율적인 대안이 있다 — 낙관적 접근이다. 방해 없이 완료할 수 있기를 바라며 그냥 업데이트를 진행하고, 업데이트 중 다른 쪽의 방해가 있었는지는 충돌 감지로 판정한다. 방해가 있었다면 연산은 실패하며, 재시도할 수 있다.

**두 기법이 갈리는 지점은 "언제 판정하는가"다.** 비관적 락은 **쓰기 전에** 배타성을 확보한다. 낙관적 락은 **쓰는 순간에** 충돌을 감지한다. 보장하는 것과 비용이 전부 이 한 가지 차이에서 파생된다.

**비관적 락이 보장하는 것.** 임계 영역에 들어간 스레드는 그 안의 복합 동작이 쪼개지지 않는다는 것(원자성)과, 락을 해제할 때의 쓰기가 다음에 그 락을 얻는 스레드에게 보인다는 것(가시성)을 함께 얻는다. 3장의 정리대로 "락은 가시성과 원자성을 모두 보장할 수 있다. volatile 변수는 가시성만 보장한다." 더 중요한 건 **진행의 보장**이다. 경쟁이 생기면 락이 알아서 호출자를 블록시켜 두었다가 차례가 오면 깨운다. 호출자 코드에는 경쟁을 다루는 분기가 아예 없다.

**비관적 락이 비용으로 치르는 것.** 15.1절이 네 가지를 열거한다. 첫째, 경쟁이 생기면 JVM이 OS의 도움을 받고 어느 스레드는 중단된 뒤 나중에 재개되는데, 재개될 때 다른 스레드들이 스케줄링 퀀텀을 다 쓰기를 기다려야 할 수도 있다. 11.3.3절은 이것을 **컨텍스트 스위치 두 번** 비용으로 못 박는다 — 블록된 스레드는 퀀텀이 끝나기 전에 쫓겨나고 자원이 풀린 뒤 다시 들어온다. 락을 쥔 쪽도 해제할 때 OS에 "블록된 스레드를 깨워 달라"고 요청해야 한다. 둘째, 락을 쥔 스레드가 페이지 폴트나 스케줄링 지연으로 멈추면 **그 락이 필요한 모든 스레드가 함께 멈춘다.** 쥔 쪽이 저우선순위이고 기다리는 쪽이 고우선순위면 고우선순위 스레드가 실질적으로 강등되는 **우선순위 역전**이 된다. 셋째, 쥔 스레드가 영구히 블록되면(무한 루프, 교착상태, 라이브락) 기다리는 쪽은 영원히 진행하지 못한다. 넷째, 확장성이다. 배타 락이 보호하는 자원의 접근은 직렬화되고, 11.4절은 이것을 "동시성 애플리케이션에서 확장성을 위협하는 주된 요인은 배타적 자원 락이다"라고 단정한다. 11.4.1절의 산수가 그 크기를 보여 준다 — 한 연산이 2ms 동안 락을 쥐고 모든 연산이 그 락을 거치면, 프로세서가 몇 개든 처리량 상한은 **초당 500건**이다.

**낙관적 락이 보장하는 것.** "조용히 덮어쓰이지 않음"을 보장한다. 더 정확히는 **성공을 보장하지 않고 충돌의 감지를 보장한다.** CAS는 "V가 A일 것 같다. 그렇다면 B를 넣고, 아니면 바꾸지 말고 내가 틀렸다고 알려 달라"는 연산이고(15.2.1), 마지막으로 값을 확인한 뒤 다른 스레드가 그것을 바꿨다면 실패를 감지한다. 그래서 lost update는 막지만, 내 업데이트가 반영된다는 약속은 하지 않는다. 그 대가로 얻는 것이 **블로킹의 제거**다. 경쟁에서 진 쪽은 중단으로 처벌받지 않고 "이번엔 못 이겼지만 다시 시도할 수 있다"는 말을 듣는다. 블록되지 않으므로 재시도할지, 다른 복구 동작을 할지, 아무것도 안 할지를 호출자가 고를 수 있고, 이 유연성이 락의 활성성 위험 대부분을 없앤다.

**낙관적 락이 비용으로 치르는 것.** 15.2.2절이 한 문장으로 정리한다. "CAS의 주된 단점은 경쟁 처리를 호출자에게 떠넘긴다는 것이다(재시도하거나, 물러나거나, 포기하거나). 반면 락은 락이 사용 가능해질 때까지 블록하면서 경쟁을 자동으로 처리한다." 즉 비관적 락에서 OS가 대신 해 주던 일이 **내 코드의 책임**으로 넘어온다. 재시도 루프, 백오프, 포기 시점, 실패를 사용자에게 어떻게 보일지가 전부 내가 설계할 대상이 된다. 그리고 경쟁이 극심하면 재시도가 역효과를 낸다. 15.3.2절에 따르면 CAS 기반 알고리즘은 경쟁에 **즉시 다시 시도하는 것으로** 반응하는데, 이것이 보통은 옳지만 고경쟁 환경에서는 경쟁을 더 만든다. 반대로 락은 스레드를 중단시켜 CPU 사용과 공유 메모리 버스의 동기화 트래픽을 **줄이는** 방향으로 반응한다. 같은 절 각주의 고백이 마지막 비용이다 — "실은 CAS의 가장 큰 단점은 그것을 둘러싼 알고리즘을 올바르게 짜기가 어렵다는 점이다."

**그래서 선택 기준.** 15.3.2절의 벤치마크 결론이 그것이다. "낮거나 중간 수준의 경쟁에서는 원자적 변수가 더 나은 확장성을 제공하고, 높은 경쟁에서는 락이 더 나은 경쟁 회피를 제공한다." 다만 책은 그 고경쟁 구간이 비현실적으로 높다고 단서를 달고, 실무의 전형적 경쟁 수준에서는 원자적 변수가 더 잘 확장된다고 정리한다. 여기서 흔한 오해 하나가 깨진다. 락을 쓰면 CAS를 안 쓰는 것이 아니다. 경쟁 없는 락 획득의 빠른 경로도 최소 한 번의 CAS를 필요로 하고, 그 비용이 대략 **CAS 비용의 두 배**다. 책의 표현대로 "락을 쓰는 것은 CAS를 보이지 않는 곳으로 옮기지만 실제 실행 비용을 절약해 주지는 않는다."

**본인 언어로 정리하면 이렇다.** 비관적 락은 **정확성과 진행을 함께 사면서 처리량을 지불한다.** 직렬화되는 구간이 생기고, 그 구간의 길이가 전체 처리량의 천장이 된다. 낙관적 락은 **처리량을 사면서 진행의 보장을 포기한다.** 충돌을 알려 줄 뿐이고, 그 다음을 내가 설계해야 하며 경쟁이 심하면 재시도가 경쟁을 키운다. 그래서 판정 기준은 "어느 쪽이 더 안전한가"가 아니라 **"이 자원에서 충돌이 실제로 얼마나 자주 일어나는가"** 다. 드물면 미리 잠그는 비용이 낭비이고, 잦으면 재시도 비용이 낭비다.

**한정 — 이 책의 범위.** 위의 모든 설명은 **하나의 JVM 안**, 한 프로세스의 힙을 공유하는 스레드 사이의 이야기다. 선착순 쿠폰이나 재고 차감의 진짜 상태는 JVM이 아니라 DB에 있고, 애플리케이션 인스턴스가 두 대가 되는 순간 `synchronized`나 `ReentrantLock`의 보장은 **그 인스턴스 안에서만** 유효해진다. DB 레벨로 올라가면 비관적 락은 `SELECT ... FOR UPDATE`로 행을 먼저 잠그는 것이고, 낙관적 락은 version 컬럼을 두고 `UPDATE ... WHERE id = ? AND version = ?`의 **영향 행 수가 0인지로 충돌을 감지**하는 것이다. 구조는 15.2절의 대비와 정확히 같다 — 쓰기 전에 배타성을 확보하느냐, 쓰는 순간에 충돌을 감지하느냐. 다만 비용의 종류가 바뀐다. 블로킹은 OS 스레드 중단이 아니라 DB 커넥션 점유와 락 대기 타임아웃이 되고, 재시도는 CPU 스핀이 아니라 트랜잭션 재실행이 된다. 이 부분은 『자바 병렬 프로그래밍』이 다루지 않으므로, F-3에서 MySQL InnoDB의 격리 수준과 함께 따로 확인해야 한다.

---

### 인용 출처

- 나심 니콜라스 탈레브, *Antifragile: Things That Gain from Disorder*, Random House, 2012. 인용은 원서 전문에서 확인했고 위치는 장·절 제목으로 표기했다. 프롤로그는 저자 본인 사이트(fooledbyrandomness.com/prologue.pdf)에 공개된 원문으로 교차 확인했다. 번역서 『안티프래질』(와이즈베리)의 문구와 쪽수는 다를 수 있다.
- Brian Goetz et al., *Java Concurrency in Practice*, Addison-Wesley, 2006. 쪽수는 원서 기준이다. 번역서는 『자바 병렬 프로그래밍』(에이콘, 2008, 강철구 역).
