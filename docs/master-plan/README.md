# DMI consolidated master plan

These files are **proposal documents**, not executed features or final approved policy. Place all four in `docs/master-plan/` of the existing repository, commit them to `main` as planning documentation after review, and keep their version/date. Then implement B3 on its own feature branch, with a draft PR and explicit test/evaluation evidence.

Files:
- `01_Master_Architecture_and_Decision_Record.md`: product thesis, invariants, architecture, key trade-offs.
- `02_B3_to_B7_Implementation_Contracts.md`: entire *conditional* remaining roadmap, B3 technical contract and acceptance gates.
- `03_Cursor_B3_Execution_Contract.md`: a single fresh Cursor Agent task after documentation is committed.
- `04_New_Chat_Handover.md`: copy to a fresh ChatGPT conversation to preserve continuity.

Workflow: **master plan → single bounded batch → tests/evaluation → draft PR → human review → update master plan**. Never give Cursor one giant B3–B7 execution request. Decide B4+ scope based on evaluation rather than a fixed deadline.
