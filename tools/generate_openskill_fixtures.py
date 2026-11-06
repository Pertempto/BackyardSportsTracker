"""Generate independent Java fixtures from a pinned openskill.py source checkout.

Usage: python tools/generate_openskill_fixtures.py /path/to/openskill.py > fixtures.json
No app code is imported. See docs/openskill-experiment.md for provenance/licenses.
"""
import json
import pathlib
import subprocess
import sys

REVISION = "65a675d21d9a0d5405731385e83be9aa781537ec"
source = pathlib.Path(sys.argv[1]).resolve()
assert subprocess.check_output(["git", "-C", str(source), "rev-parse", "HEAD"], text=True).strip() == REVISION
assert not subprocess.check_output(["git", "-C", str(source), "status", "--porcelain"], text=True).strip()
sys.path.insert(0, str(source))
from openskill.models import BradleyTerryFull, PlackettLuce

cases = []
for equal in (True, False):
    for gap in (0, 1, 2, 3, 10):
        cases.append({"name": f"{'equal' if equal else 'asymmetric'}-gap-{gap}",
                      "teams": [[1], [2]], "scores": [10, 10 - gap],
                      "priors": [[25, 25 / 3], [25, 25 / 3]] if equal else [[35, 4], [20, 7]],
                      "weights": [[1], [1]]})
cases += [
    {"name": "uneven-upset", "teams": [[1, 2], [3]], "scores": [1, 10],
     "priors": [[32, 3], [18, 8], [24, 5]], "weights": [[1, 1], [1]]},
    {"name": "explicit-weights", "teams": [[1, 2], [3, 4]], "scores": [10, 2],
     "priors": [[30, 4], [20, 7], [23, 6], [25, 3]], "weights": [[0.5, 1.5], [0.75, 1.25]]},
    {"name": "asymmetric-draw", "teams": [[1, 2], [3]], "scores": [0, 0],
     "priors": [[30, 4], [20, 7], [23, 6]], "weights": [[1, 1], [1]]},
]
fixtures = []
for case in cases:
    for cls in (BradleyTerryFull, PlackettLuce):
        for margin in (0, 2):
            model = cls(margin=margin, weight_bounds=None)
            ids = [i for team in case["teams"] for i in team]
            priors = dict(zip(ids, case["priors"]))
            teams = [[model.rating(mu=priors[i][0], sigma=priors[i][1]) for i in team] for team in case["teams"]]
            result = model.rate(teams, scores=case["scores"], weights=case["weights"])
            fixtures.append(dict(case, model=cls.__name__, margin=margin,
                                 expected=[[[p.mu, p.sigma] for p in team] for team in result]))
print('{"revision": ' + json.dumps(REVISION) + ', "fixtures": [\n'
      + ',\n'.join(json.dumps(fixture) for fixture in fixtures) + '\n]}')
