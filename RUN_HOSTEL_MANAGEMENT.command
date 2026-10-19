#!/bin/zsh
# One-click launcher for the Hostel Management System.
# Copy this file beside pom.xml in the HostelManagementSystem project root.

cd "$(dirname "$0")" || exit 1

if [[ ! -f "pom.xml" ]]; then
  echo "ERROR: pom.xml was not found. Place this launcher beside pom.xml."
  read "?Press Return to close..."
  exit 1
fi

# Load common zsh environment configuration when available so tools and
# environment variables configured there are available to a Finder launch.
if [[ -f "$HOME/.zshrc" ]]; then
  source "$HOME/.zshrc" 2>/dev/null
fi

echo "=============================================="
echo " Starting Hostel Management System"
echo "=============================================="
echo "Compiling changed Java files and launching..."
echo
mvn compile exec:java -Dexec.mainClass=com.hostelmanagement.Main
status=$?
echo
if [[ $status -ne 0 ]]; then
  echo "The application did not start successfully. Check the Maven error above."
else
  echo "The application has closed."
fi
read "?Press Return to close this window..."
exit $status
