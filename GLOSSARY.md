# Saudagar

The language of the game: a trader riding trains between six Indian cities in the year 2000,
buying where goods are cheap and selling where they are dear, for as many days as the money
lasts. Code, strings and docs should all use these terms and nothing else.

## The run

**Run**:
One playthrough, from the first train to an **Ending**. Ends by choice, by poverty, or by the **Collector** — never by a day limit.
_Avoid_: game, session, playthrough

**Day**:
The unit a run is measured in; "Day N" is days survived. Most actions are free; a day passes when the train rolls, a **Bandh** strands the trader, or an action bills it.
_Avoid_: turn, tick, round

**Net worth**:
Everything the trader owns minus everything owed: cash plus savings plus the resale value of goods, less debt. The number the **Rank** ladder measures.

**Rank**:
The title net worth earns: thelawala at the bottom, then dukandaar, **saudagar**, seth and crorepati. Below zero the rank is divala — broke.

**Ending**:
How a run closes: retired (ended by choice), died broke (nothing left and no way to borrow), or collected (the **Collector** came and was not paid).

## Money and debt

**Sahukar**:
The moneylender in every city. Lends to the debt-free only, one loan at a time, capped at ₹50,000, at 8% per day the loan stands.
_Avoid_: lender, loan shark, bank (that word is taken)

**Debt**:
What is owed. Taken by borrowing, or landed on unpaid when cash cannot cover a bill. Either way it starts a ten-day clock.

**Collector**:
The sahukar's enforcer, who arrives the day the debt clock runs out. He settles the whole debt plus the day's keep, cash first then savings — or the run ends.

**Living cost**:
The per-city price of staying alive, billed whenever a day passes. In the **Collector**'s words it is "the day's keep" — same thing.
_Avoid_: rent, upkeep, expense

**Bank**:
Where savings sit: safe from **Pickpocket**s and **Checkpost**s, earning a little interest, reachable from any city.

## The mandi

**Mandi**:
The city's market, where goods are bought and sold.
_Avoid_: market, exchange, bazaar

**Good**:
A tradable commodity — onions, chai, basmati, cotton, silk sarees, kesar.
_Avoid_: item, product, commodity (in speech — the code may say it)

**Price shift**:
The way a mandi's price moves as goods move: every unit bought nudges the price up, every unit sold nudges it down. Big orders push against themselves.
_Avoid_: slippage, price impact

**Speciality**:
The good a city sells cheapest — the one worth buying there. Delhi's basmati, Srinagar's kesar, and so on.

**Movers**:
The day's biggest price movers in the local mandi — the board's loudest signal of what changed overnight.

**Rumour**:
Word on the street that a good will move in some city within days. Most land as real swings; some are duds, and there is no way to know which until the days pass.
_Avoid_: tip, forecast, news

## The world

**City**:
One of the six places the rail network joins. Each has its own mandi prices, living cost and speciality.

**Transport**:
What carries the goods — the ladder is jhola, trunk, tempo, truck, each a bigger hold for a bigger price.
_Avoid_: luggage, capacity (mechanism words; the thing bought is a transport)

**Godown**:
A rented warehouse, one per city, where goods sit safe from theft at a per-city daily rent billed when the train rolls.
_Avoid_: warehouse, storage (in speech)

## The rails

**Event**:
Chance that meets the trader on the road or in town: a **Pickpocket**, a **Checkpost**, a **Bandh**, a **Rumour** or a **PriceSwing**.

**Bandh**:
A strike that strands the trader in place for days while living costs, interest and godown rent keep ticking.

**Pickpocket**:
A thief who lifts a share of the carried cash.

**Checkpost**:
A stop where the carried goods are taxed.

**Price swing**:
A sudden move in one good's price in one city — the thing a true **Rumour** forecasts.
