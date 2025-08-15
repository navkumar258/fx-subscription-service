tags=($(git tag --sort=creatordate))

for i in "${!tags[@]}"; do
  tag="${tags[$i]}"
  artifact_path="artifacts/$tag/artifact.zip"

  # Get changelog: for first tag, get all up to tag; otherwise, get commits between tags
  if [ $i -eq 0 ]; then
    changelog=$(git log --oneline "${tag}")
  else
    prev_tag="${tags[$i-1]}"
    changelog=$(git log --oneline "${prev_tag}".."${tag}")
  fi

  release_notes="## Changelog

$changelog
"

  # Only create if release doesn't already exist
  if ! gh release view "$tag" &>/dev/null; then
    echo "Creating release for $tag"
    if [ -f "$artifact_path" ]; then
      gh release create "$tag" --title="$tag" --notes "$release_notes" "$artifact_path"
      echo "  -> Attached asset: $artifact_path"
    else
      gh release create "$tag" --title="$tag" --notes "$release_notes"
      echo "  -> No asset found for $tag, released changelog only."
    fi
  else
    echo "Release for $tag already exists. Skipping."
  fi
done
