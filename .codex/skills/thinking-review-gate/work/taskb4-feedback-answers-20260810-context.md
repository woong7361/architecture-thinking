# Project context

- Target: `task2/assignments/taskB-4.md`.
- Preserve every feedback original. Add `**답변:**` blocks beneath substantive comments, following `taskB-3.md` and other assignments.
- Six feedback entries exist. FB-B4-03 is only agreement and introduces no question, condition, counterexample, or change request, so it remains without an answer under the analyze-task-feedback selection rule.
- FB-B4-01 asks how to control and track cognitive load caused by SRP-driven object decomposition.
- FB-B4-02 follows the original claim that a vendor change only requires a new adapter. The current `ChargePort.charge(...): boolean` encodes synchronous completion. The user interprets the deeper question as whether one model can accept synchronous and asynchronous vendors from the outset and whether doing so is overdesign.
- FB-B4-04 has an ambiguous anchor. It may refer to the core `UserRepository` interface or the concrete `DB UserRepo` box.
- FB-B4-05 asks why `TicketRepository` is not named Port.
- The user chose to make Port and Adapter roles explicit for FB-B4-04 and FB-B4-05 because their N-tier experience tended to encourage data-centered thinking. Preserve the nuance that a core-owned Repository can still be a valid outbound port, and naming alone does not create OOP.
- FB-B4-06 asks whether three primitive parameters are the best inbound use-case contract.
- Existing B-2 code has a synchronous boolean payment API and a data-oriented service that asks `Ticket.isReserved()` and uses setters. The refactored code keeps persistence interfaces but moves reservation decisions and state transitions into `Ticket.ensureReservable()` and `Ticket.assignTo()`.
- Do not change the original task answer or feedback metadata because their stored line anchors would become stale. Only append feedback answers.
- Do not answer FB-B4-03, matching other task handling of praise/reactions without substantive issues.
