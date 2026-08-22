import sys, os, urllib.request

scriptdest = "../youtube-dl"
name = "yt-dlp"
url = "https://github.com/yt-dlp/yt-dlp-nightly-builds/releases/latest/download/yt-dlp"

# Clean up old files first
for path in (scriptdest, "youtube-dl", name):
    try:
        os.unlink(path)
    except:
        pass

print("Downloading '%s'..." % url)
try:
	urllib.request.urlretrieve(url, name)
except urllib.error.HTTPError as e:
	print(e)
	exit(1)
except urllib.error.ContentTooShortError as e:
	print("Download interrupted!")
	exit(1)
print("OK.")

# Write wrapper script
with open(scriptdest, "w") as f:
	f.write("""#!/system/bin/sh\nexec "$(dirname "$0")/ytdl/wrapper" %s "$@"\n""" % name)
os.chmod(scriptdest, 0o700)
