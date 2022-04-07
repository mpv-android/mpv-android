import sys, re
with open(sys.argv[1], "r") as f:
	lines = f.readlines()
for i, l in enumerate(lines):
	if not l.startswith("#") or l[1] in " \t\n" or re.match(sys.argv[2], l[1:]): continue
	elif l.startswith("#*shared*"): continue
	elif l.startswith("#*disabled*"): break
	lines[i] = l.lstrip("#")
	while lines[i].strip().endswith("\\"):
		i += 1
		lines[i] = lines[i].lstrip("#")
with open(sys.argv[1], "w") as f:
	f.writelines(lines)
