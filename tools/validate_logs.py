"""Check Spire Coach NDJSON logs for readable, internally consistent events.

This validates the data pipeline, not whether a coaching recommendation was
strategically correct. Use it against the game's local logs directory.
"""

import argparse
import json
import sys
from collections import Counter
from pathlib import Path


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--log-dir", required=True, type=Path,
                        help="Path to the game's logs/spirecoach directory")
    args = parser.parse_args()

    if not args.log_dir.is_dir():
        print("Log directory does not exist: {}".format(args.log_dir), file=sys.stderr)
        return 2

    files = sorted(args.log_dir.glob("*.ndjson"))
    if not files:
        print("No NDJSON files found in {}".format(args.log_dir), file=sys.stderr)
        return 2

    event_counts = Counter()
    errors = []
    warnings = []
    run_files = 0
    run_ends = []
    card_rewards = 0

    for path in files:
        if path.name.startswith("run-"):
            run_files += 1
        try:
            lines = path.read_text(encoding="utf-8").splitlines()
        except OSError as error:
            errors.append("{}: could not read file: {}".format(path.name, error))
            continue

        for line_number, line in enumerate(lines, start=1):
            if not line.strip():
                continue
            try:
                record = json.loads(line)
            except json.JSONDecodeError as error:
                errors.append("{}:{}: invalid JSON ({})".format(path.name, line_number, error.msg))
                continue
            if not isinstance(record, dict):
                errors.append("{}:{}: event is not an object".format(path.name, line_number))
                continue

            event = record.get("event")
            if not isinstance(event, str) or not event:
                errors.append("{}:{}: missing event name".format(path.name, line_number))
                continue
            event_counts[event] += 1

            if event == "card_reward":
                card_rewards += 1
                validate_card_reward(path, line_number, record, errors, warnings)
            elif event == "run_end":
                run_ends.append((path, line_number, record))
                validate_run_end(path, line_number, record, errors)

    print("Files checked: {}".format(len(files)))
    print("Events:")
    for event, count in sorted(event_counts.items()):
        print("  {:<18} {}".format(event, count))
    print("Run logs: {} | run summaries: {} | card rewards: {}".format(
        run_files, len(run_ends), card_rewards))

    if not run_files:
        warnings.append("No run-*.ndjson files found; run-level validation has not been exercised yet.")
    if run_files and not run_ends:
        warnings.append("Run logs exist but none has a run_end event; the latest run may still be active or ended before this feature was installed.")

    for warning in warnings:
        print("WARNING: {}".format(warning))
    for error in errors:
        print("ERROR: {}".format(error), file=sys.stderr)

    if errors:
        print("Validation failed with {} error(s).".format(len(errors)), file=sys.stderr)
        return 1
    print("Validation passed: log structure is readable and internally consistent.")
    print("This does not prove that coaching advice is strategically correct.")
    return 0


def validate_card_reward(path, line_number, record, errors, warnings):
    prefix = "{}:{}".format(path.name, line_number)
    offered = record.get("offeredIds")
    if not isinstance(offered, list) or not all(isinstance(item, str) for item in offered):
        errors.append("{}: offeredIds must be a list of strings".format(prefix))
        offered = []

    skipped = record.get("skipped")
    selected = record.get("selectedId")
    if not isinstance(skipped, bool):
        errors.append("{}: skipped must be boolean".format(prefix))
    elif skipped and selected is not None:
        errors.append("{}: skipped reward has selectedId={!r}".format(prefix, selected))
    elif not skipped and selected not in offered:
        errors.append("{}: selectedId={!r} is not in offeredIds".format(prefix, selected))

    before = record.get("deckSizeBefore")
    after = record.get("deckSizeAfter")
    if not isinstance(before, int) or not isinstance(after, int):
        errors.append("{}: deck sizes must be integers".format(prefix))
    elif skipped and after != before:
        warnings.append("{}: skipped reward changed deck size {} -> {}".format(prefix, before, after))
    elif not skipped and after != before + 1:
        warnings.append("{}: selected reward changed deck size {} -> {} (expected +1)".format(prefix, before, after))


def validate_run_end(path, line_number, record, errors):
    prefix = "{}:{}".format(path.name, line_number)
    names = ("combatsReviewed", "lessonsFound", "cardRewards", "rewardsSkipped", "cardsAdded")
    values = {}
    for name in names:
        value = record.get(name)
        if not isinstance(value, int) or value < 0:
            errors.append("{}: {} must be a non-negative integer".format(prefix, name))
        else:
            values[name] = value
    if len(values) == len(names) and values["rewardsSkipped"] + values["cardsAdded"] != values["cardRewards"]:
        errors.append("{}: card reward counters do not add up".format(prefix))


if __name__ == "__main__":
    sys.exit(main())
