# Experimental two-team skill ratings

## Status and licensing gate

- This is a **developer-only, read-only experiment**, compiled from `app/src/debug` and absent from release builds; it is not selected by any screen or production rating path.
- Do not enable it for production or distribute debug builds containing it without an IP review. Debug-only placement is a release guard, **not a legal safe harbor** for research, use, or distribution.
- No third-party rating implementation is bundled. Independently implementing the equations does not itself resolve patent rights, and this repository's MIT license does not grant Microsoft's rights.

### Primary-source review (2026-11-06)

1. Microsoft's [TrueSkill research page](https://www.microsoft.com/en-us/research/project/trueskill-ranking-system/) describes Gaussian beliefs, team sums, outcome-only updates, dynamics, draws, and conservative rankings. The current page does **not** supply a general algorithm/patent license or a verifiable blanket noncommercial permission. Do not treat the noncommercial/Xbox-only statements in third-party library documentation as an applicable Microsoft license.
2. Microsoft's [GDK copyright notice](https://learn.microsoft.com/en-us/gaming/gdk/docs/gdk-dev/intro/copyright) explicitly identifies TrueSkill as a Microsoft trademark and says: “Except as expressly provided in any written license agreement from Microsoft, the furnishing of this document does not give you any license to these patents, trademarks, copyrights, or other intellectual property.” This is a notice, not a commercial algorithm license or a determination of which patent claims apply here.
3. Microsoft's [Trademark and Brand Guidelines](https://www.microsoft.com/en-us/legal/intellectualproperty/trademarks) permit certain truthful wordmark references without implying endorsement, restrict product/app naming and branding, and require permission for uses outside the guidelines. These references describe the experiment; they do not brand the app as a Microsoft product. TrueSkill is a trademark of the Microsoft group of companies.
4. The cited patent document, [US8538910B2, “Determining relative skills of players”](https://patents.google.com/patent/US8538910B2/en), describes Bayesian player/team skill updates. Google hosts the patent text and lists Microsoft Technology Licensing LLC, active status, and adjusted expiry 2029-04-09, **but explicitly disclaims a legal analysis of status**. This is not independently verified official-register status, an exhaustive family search, or a claim-by-claim applicability opinion; neither that expiry nor Wikipedia establishes freedom to operate.

**Production blocker:** obtain counsel's jurisdiction-specific patent/family and trademark assessment and any required written Microsoft permission/license before adopting this algorithm. Confirm intended distribution and commercial use with that review. No production adoption is authorized by this experiment. Microsoft lists `trueskill@microsoft.com` on the research page for questions; its trademark guidelines provide `trademarks@microsoft.com` for branding questions.

## Model integration

- Input is the app's existing `List<Game>`, including rosters as stable player IDs, sport, scores, deleted flag, and game ID. No Room schema, backup format, player rating, game snapshot, or existing UI changes.
- Build one model per supported sport; null and `Sports.ALL` are rejected rather than pooling unrelated skills. Unknown/new players start at mean 25 and standard deviation 25/3. The existing scalar rating is deliberately not converted into a Gaussian prior.
- Replay active games in ascending ID order, matching `GameHistoryReplayer`. Rebuild after score, roster, or deletion changes. Dates do not reorder history. Caller lists and game/player objects are not mutated.
- Exactly two nonempty teams are supported, including unequal team sizes. Reject duplicate/overlapping player IDs and negative scores. Every listed player contributes equally for the whole game; no partial participation is inferred from unavailable data.
- Team skill is the sum of player skills. Individual beliefs follow players across changing rosters; high-uncertainty players move more. Constant lineups cannot identify individual contributions reliably. There are no teammate synergy terms.
- Classic TrueSkill uses only win/loss/draw, not score margin. A recorded 0–0 is a draw, unlike the existing scalar algorithm's no-op. Unplayed games must not be recorded as 0–0 results.
- Provisional parameters are beta 25/6, per-appearance dynamics tau 25/300, and draw probability 10% (normal quantile at 0.55). These are conventional defaults, **not calibrated sport-specific estimates**. Sports have separate beliefs but share these experimental parameters. Inactivity does not add elapsed-time decay.
- Two-team Gaussian update moments are evaluated by 1,024-panel Simpson integration relative to the truncated normal's mode, avoiding subtraction of tiny CDF tails. Integration is limited to 12 standard deviations from that mode (omitted density at most exp(-72) relative to the mode). This is a numerical approximation, not the multi-team message-passing implementation or TrueSkill 2.
- `rating(id)` exposes mean, sigma, and `conservative()` = mean − 3 × sigma. `matchQuality(team1, team2)` returns the standard uncertainty-aware hypothetical-draw quality in [0, 1]; it is **not a win probability**. Higher quality is preferable when comparing candidate groupings, not lower conservative-score difference.

### Try it from debug code or a JVM test

```java
ExperimentalTrueSkill model = ExperimentalTrueSkill.fromGames(games, Sports.BASKETBALL);
ExperimentalTrueSkill.Rating belief = model.rating(player.id);
double conservative = belief.conservative();
double quality = model.matchQuality(grouping.team1, grouping.team2);
```

No UI toggle is provided while the licensing gate is unresolved. The API consumes the same histories and candidate rosters as the app's existing team-balancing code so it can be evaluated alongside that code without replacing it.

## Comparison with current approaches

| Property | Persisted `Game` / player ratings | `TeamStrength` (current grouping generator) | Experiment |
| --- | --- | --- | --- |
| Evidence | Score share versus prior team-rating share | Equal-weight fit of score margins | Sequential win/loss/draw |
| Skill | Scalar, multiplicative update | Regularized player weights and signed teammate effects | Individual Gaussian mean and uncertainty |
| New player | Existing/manual scalar | Neutral contribution of 1 | 25 ± 8.333 standard deviation |
| Team changes | Same player scalar | Shared players and pair effects | Same player belief across rosters |
| Time | Sequential updates | Full-history equal weight | Sequential, per-appearance dynamics |
| Grouping objective | Not the current grouping objective | Minimize fitted strength gap | Could maximize match quality; not wired into production |
| Sport separation | Sport-keyed ratings | Selected-sport history | Mandatory single-sport replay |

Reproducible, equal-prior 1v1 basketball examples (each starts afresh at scalar 1000 or Gaussian 25, 25/3):

| Result | Existing winner scalar | Experimental winner mean | Experimental sigma |
| --- | ---: | ---: | ---: |
| 10–9 | 1010.526316 | 29.395832 | 7.171476 |
| 10–0 | 1200.000000 | 29.395832 | 7.171476 |
| 0–0 draw | 1000.000000 | 25.000000 | 6.457516 |

Tests also show that `TeamStrength` assigns a larger fitted gap to the blowout, whereas the experiment intentionally gives both wins identical beliefs. After A+B beat C+D, then A+C beat B+D, the experiment produces means A=30.972955, B=25.243689, C=24.756311, D=19.027045, all with sigma 7.268023. This checks that beliefs follow individuals rather than fixed teams.

These are synthetic contract comparisons, **not evidence of better predictive performance on backyard sports**. No real user history was available or used. Before changing the app's default, evaluate forward-only held-out outcomes, calibration, and grouping quality per sport, including newcomer-heavy games, unequal rosters, stable versus rotating teammates, and scoring-rule changes. Decide whether losing score-margin and teammate-synergy information is acceptable.

## Verification

Run with the repository's Java 8 / Android 28 environment:

```sh
bash ./gradlew --no-daemon testDebugUnitTest assembleDebug lintDebug assembleRelease testReleaseUnitTest
```

- Eight debug-only experiment tests cover independent numeric fixtures, ties including 0–0, uneven-team upsets, side symmetry, changing teams, uncertainty, sport isolation, edits/deletions, non-mutation, quality ordering, long histories, and invalid inputs, alongside the existing tests.
- Numeric fixtures were generated separately using Python `trueskill==0.4.5` with its `mpmath` backend and default parameters; assertions allow absolute error 0.000001. That library is a validation tool only, not an app dependency or evidence of algorithm licensing.
- Observed result: `BUILD SUCCESSFUL`; 34 debug tests and 26 release tests, all passing with no skips; debug/release APK assembly and debug lint passed.
- DEX inspection found `ExperimentalTrueSkill` in the debug APK and no such classes in the release APK. No Android device execution or UI changes were involved.

## Decisions before further integration

1. Resolve IP clearance, including whether further experimental distribution itself needs permission.
2. Decide whether outcome-only learning is preferable to the current score-margin and teammate-effect model using representative, forward-held-out sport histories.
3. Calibrate draw frequency/performance variance per sport and define how to represent unplayed, forfeited, or partially played matches before exposing a user-facing selector or persisting beliefs.
