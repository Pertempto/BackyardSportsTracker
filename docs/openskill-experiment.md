# OpenSkill comparison experiment

## Status

- All Java additions live in `app/src/debug`; fixtures/tests live in `app/src/testDebug`. Production screens, ratings, balancing, database, and backup formats are unchanged. Nothing is selected by a UI toggle.
- `ExperimentalOpenSkill` is a **two-team specialization**, adapted from the MIT-licensed Python reference, not a dependency on the Java/Kotlin packages and not a complete OpenSkill port. The Python copyright/license is retained in [debug assets](../app/src/debug/assets/licenses/openskill-python.txt) and packaged with the debug APK.
- The [existing TrueSkill licensing gate](trueskill-experiment.md#status-and-licensing-gate) still applies. Debug source isolation does not grant permission to distribute or use patented technology.

## Dependency and license evaluation (2026-11-06)

| Source inspected | Compatibility and feature findings | Decision |
| --- | --- | --- |
| [pocketcombats/openskill-java](https://github.com/pocketcombats/openskill-java/tree/db744b44c6e3ad551bcc0ca31154cad3d5536b56), revision `db744b44c6e3ad551bcc0ca31154cad3d5536b56` | POM targets Java 16; records, streams, `Stream.toList()`, and no-argument `Optional.orElseThrow()` occur in production sources. Project currently builds with Java 8 / AGP 3.3.2 / minSdk 19. Java 8 compilation of `SimplePlayerResult` fails at `public record`. BT, PL, ranks/ties, individual update weights, dynamics, balance and quality evaluation exist; no score/margin input or margin configuration appears in the rating models. `TeamResult.weight()` is not used by the inspected update/model path, so it is not score-margin support. | **Not a drop-in Android dependency for this app.** No claim that all modern Android toolchains are incompatible: a modern toolchain/desugaring audit or backport would be separate work. No dependency/toolchain upgrade made. |
| [vivekjoshy/openskill.py](https://github.com/vivekjoshy/openskill.py/tree/65a675d21d9a0d5405731385e83be9aa781537ec), revision `65a675d21d9a0d5405731385e83be9aa781537ec` (source reports 6.2.0) | Python 3.10+ is not an Android runtime dependency here. Supplies BT Full, PL, score margins, explicit weights, predictions, ties, and multi-team behavior. | Independent fixture oracle plus attributed source for a narrow Java-compatible two-team adaptation; no Python runtime bundled. |
| [brezinajn/openskill.kt](https://github.com/brezinajn/openskill.kt) | GitHub license metadata is null; inspected repository tree and README contain no license grant. Absence of a detected license is not proof that no permission could ever be obtained. | **Not adopted or copied.** Permission remains unverified; do not infer MIT permission from a related JavaScript repository. |

### Primary-source terms and unresolved IP

- The Java project's [LICENSE.txt at the inspected revision](https://github.com/pocketcombats/openskill-java/blob/db744b44c6e3ad551bcc0ca31154cad3d5536b56/LICENSE.txt) is Apache-2.0. Redistribution conditions include providing the license, retaining relevant notices, marking modified files, and carrying NOTICE content if present. Section 3 grants only contributor-licensable patent claims necessarily infringed by contributions; section 6 does not generally grant trademarks. This is not a license to unrelated third-party patents. No Java code is shipped in this experiment.
- The Python project's [LICENSE](https://github.com/vivekjoshy/openskill.py/blob/65a675d21d9a0d5405731385e83be9aa781537ec/LICENSE) is MIT, copyright 2024 Vivek Joshy. It grants use/modification/distribution subject to retaining the copyright and permission notice; it disclaims warranties including noninfringement. MIT has no express patent grant equivalent to Apache section 3. The adapted Java file identifies the source/modification, and debug APKs include that license.
- The original [Weng–Lin paper, JMLR 2011](https://www.jmlr.org/papers/v12/weng11a.html), describes the approximation algorithms. A research publication and a repository's “open license” marketing are **not** an exhaustive patent search or legal clearance. No algorithm-family/jurisdiction claim analysis was performed. The newer reference margin extensions also need consideration in any IP review; do not assume they are covered by the original paper.
- Before production adoption/distribution, resolve applicable patent/trademark questions with counsel and any relevant rights holders. This experiment verifies source-code licenses, **not freedom to operate**. No Kotlin permission, Microsoft license, or blanket legal clearance is claimed.

## Implemented model contract

- Two nonempty teams with unique, stable player IDs; reject overlapping/duplicate IDs, deleted games, negative scores, invalid parameters, and nonpositive/nonfinite weights. A 0–0 result is a draw, not a missing result.
- Pure `rate(game, prior, model, margin, weights)` returns a new belief map and leaves input games/ratings untouched; nonparticipants retain their beliefs. Callers must maintain separate maps per sport. The evaluator enforces one supported sport and ignores deleted/other-sport games.
- Defaults: mean 25, sigma 25/3, beta 25/6, per-appearance tau 25/300, kappa 0.0001, no sigma cap, no balance transformation, no weight normalization. Team means/variances are sums; **beta noise is per team in these Weng–Lin updates**, not per player as in the prior TrueSkill experiment.
- The app has only two teams. For this case BT Full and PL give the same updates without margins, including draws; both are retained to verify their **different score-margin extensions**. Multi-team rankings, partial-pairing models, reference prediction APIs, custom gamma, weight normalization, and configurable balance/sigma caps are not implemented. Candidate roster means can be compared with `forTeam`; no replacement grouping generator or win-probability calibration is claimed.

### Margin behavior: do not assume monotonic amplification

With configured margin \(m\), actual score gap \(g\), and prior team-mean difference \(d\), the pinned Python reference uses factor \(f = \log(1 + g/m)\) only when \(m > 0\) and \(g > m\); otherwise \(f = 1\). The two-team likelihood uses difference \(d/f\) for BT and \(d(3 - 2f)\) for PL. Those differences enter the logistic function; sigma updates also change through that likelihood. These are update-time transformations, not predictions using a future score.

- With equal prior team means (\(d = 0\)), margins have **no effect**, even for a blowout. Equal-prior fixtures alone would miss this feature entirely.
- At \(g = m\), the extension is off; just above the boundary \(f\) starts near log(2), below 1. Consequently an expected winner can receive a **smaller** update immediately above the threshold. This behavior is intentionally matched, not silently “fixed.”
- For PL, sufficiently large gaps make \(3 - 2f\) negative, reversing the sign of the prior difference inside the update likelihood. This can substantially amplify updates. It is not evidence that large score margins always improve learning.

Independent Python fixtures for a 35 ± 4 player beating a 20 ± 7 player, with m = 2:

| Score | BT winner mean | PL winner mean |
| --- | ---: | ---: |
| 10–9 (below threshold) | 35.291920 | 35.291920 |
| 10–8 (at threshold) | 35.291920 | 35.291920 |
| 10–7 (above threshold) | 35.260575 | 35.236596 |
| 10–0 | 35.483886 | 36.131717 |

Scores are raw sport points. Margin 2 is a test parameter, **not a calibrated common threshold across basketball, ultimate, and spikeball**. Margin 0 disables the extension. Tune only using training/validation periods, not the final test period.

### Player weights are not teammate synergy

- Python reference weights scale an individual's **update**: positive omega uses weight, negative omega uses its reciprocal (weight normalization disabled here). They do not learn a player-pair interaction or multiply the team mean in the default aggregation. Explicit asymmetric weight fixtures verify both signs and sigma updates.
- Match rosters do not record participation or individual contribution, so comparisons use weight 1 for everyone; no weights are inferred from wins, player popularity, or teammate combinations.
- `TeamStrength` already learns signed pair effects. The evaluator reports its full gap, the sum of singleton/player contributions, and their difference as the pair contribution. Pair contribution is diagnostic, **not a standalone predictor** and not a fitted OpenSkill player weight.
- The player-component comparison removes pairs **from the same joint fit**. It is a conditional contribution diagnostic, not a separately refitted player-only ablation or a causal estimate of friendship/synergy. Stable rosters can confound players and pairs. Production evidence would require rotating-roster heldouts and a separately refitted ablation before claiming synergy generalizes.

## Chronological held-out comparison

```java
ExperimentalRatingComparison.Report report = ExperimentalRatingComparison.evaluate(
        games, Sports.BASKETBALL, cutoffDate, 2.0);
```

1. Filter active games for one sport; validate both training and held-out games.
2. Sort by date, then ID for deterministic within-date ordering. Train only on dates **strictly before** cutoff; every game at/after cutoff is held out, including all games exactly at the boundary.
3. Freeze every model for the entire held-out period. Never train on test scores or use final player ratings / stored rating snapshots as priors. Scalar replay uses neutral 1000 priors, so it tests the existing update rule rather than reproducing personalized manual initial ratings. New players use each model's neutral prior.
4. Compare scalar replay, full `TeamStrength`, its player component, TrueSkill, BT, BT+margin, PL, and PL+margin. Historical copies give the TrueSkill ID-ordered replay chronological IDs without mutating originals; production history replay still uses its existing ID semantics.
5. Report directional accuracy on decisive held-out outcomes only (neutral gap within 0.000000001 receives half credit); report draw count separately. Only the full fit and its player component have score-point MAE. Gaussian/scalar gaps are not score predictions or calibrated win probabilities; no misleading cross-scale MAE, Brier score, or log loss is produced.

This tests prediction of observed roster outcomes, **not the causal effect of choosing alternative balanced teams**. Within a model, smaller absolute mean/strength gaps suggest closer teams, but this harness does not claim that different grouping objectives or uncertainty-aware matchmaking are equivalent. Repeated identical or uncertain rosters need further evaluation. Date edits or missing/unreliable dates must be resolved before treating a real-data holdout as chronological evidence.

### Available data and observed synthetic results

No real game history, database, CSV, or exported backup was available in this checkout/workspace; the JSON files found were Room schemas. No device/production data was accessed. All runs below are **synthetic validation, not predictive evidence or a recommendation to change defaults**.

The committed synthetic comparison test constructs 18 chronological training games (six rotations of A+B beating C+D 10–4, A+C beating B+D 10–8, and A+D losing to B+C 6–10). Five held-out games repeat these outcomes, add a newcomer-heavy A+E versus B+F 10–9 result, and include one draw. Parameters/cutoff are fixed before evaluating; the tiny designed dataset cannot establish superiority.

| Model | Direction accuracy (4 decisive games) | Score-margin MAE (5 games, including draw) |
| --- | ---: | ---: |
| Scalar update rule | 0.75 | Not comparable units |
| TeamStrength full | 0.75 | 0.867358 |
| Player component of same fit | 0.75 | 2.204428 |
| TrueSkill | 1.00 | Not comparable units |
| BT / PL without margins | 1.00 / 1.00 | Not comparable units |
| BT+margin / PL+margin | 0.75 / 0.75 | Not comparable units |

For the first held-out roster, full strength gap is 5.363336 = player component 1.346820 + pair component 4.016516 (actual margin 6). Pair terms reduce score error on this designed fixture, without improving directional accuracy; that does not establish generalizable synergy. Both margin models miss the newcomer game here while their no-margin versions predict its direction. These outcomes illustrate why margin/weight/synergy changes must be evaluated separately rather than assumed beneficial.

## Reproduce and verify

```sh
bash ./gradlew --no-daemon testDebugUnitTest assembleDebug lintDebug assembleRelease testReleaseUnitTest
```

- Nine new tests cover 52 independent Python mean/sigma fixtures at absolute tolerance 0.000000001 (also swapping sides), rotating rosters, uncertainty, weights, invalid inputs, date versus ID ordering, cutoff boundaries, frozen/nonleaking predictions, sport/deletion filtering, draws, and analytic separation of pair/player contributions.
- Existing tests remain unchanged. No Android device execution or UI modifications are part of this task.
- Observed result: `BUILD SUCCESSFUL`; 43 debug tests and 26 release tests passed, with zero failures, errors, or skips; debug/release assembly and debug lint passed.
- APK inspection confirmed all three experimental classes present in debug and absent in release; the exact upstream Python license is packaged in debug only. Fixtures are test resources, absent from both APKs. Fixture regeneration was byte-for-byte reproducible.

Regenerate independent fixtures outside the app using a clean, pinned source checkout (Python dependencies are tooling only):

```sh
git clone https://github.com/vivekjoshy/openskill.py /tmp/openskill-reference
git -C /tmp/openskill-reference checkout 65a675d21d9a0d5405731385e83be9aa781537ec
uv venv /tmp/openskill-fixtures-env
uv pip install --python /tmp/openskill-fixtures-env/bin/python typing-extensions==4.16.0
/tmp/openskill-fixtures-env/bin/python tools/generate_openskill_fixtures.py /tmp/openskill-reference > app/src/testDebug/resources/openskill-fixtures.json
```

The generator imports only the upstream reference, checks the revision/clean tree, and never imports app code. The saved numeric fixtures do not require Python, network access, or an OpenSkill dependency to run the Java tests.

## Decisions needed

1. Obtain representative, privacy-approved real histories and reliable dates before selecting an algorithm; use validation periods for sport-specific margin tuning and a final untouched chronological test period.
2. Decide whether the pinned reference's threshold discontinuity and PL large-margin sign reversal are acceptable experimental semantics; a redesigned margin rule would need its own name, fixtures, and evaluation rather than a parity claim.
3. Keep per-player uncertainty, explicit update weights, and pair synergy as distinct hypotheses; add a separately refitted player-only ablation if real histories support assessing synergy.
4. Resolve IP questions before production adoption/distribution. Do not adopt the unlicensed Kotlin source or require a Java/Android toolchain upgrade solely for this experiment.
