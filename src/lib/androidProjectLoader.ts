// Loads all Android Studio Gradle project files via Vite's import.meta.glob
// so users can browse every file and download a complete buildable ZIP archive.

import { buildZipArchive } from './zipBuilder';

const rawTextModules = import.meta.glob(
  [
    '/build.gradle.kts',
    '/settings.gradle.kts',
    '/gradle.properties',
    '/gradlew',
    '/gradlew.bat',
    '/README_ANDROID.md',
    '/gradle/libs.versions.toml',
    '/gradle/wrapper/gradle-wrapper.properties',
    '/app/build.gradle.kts',
    '/app/proguard-rules.pro',
    '/app/src/**/*.kt',
    '/app/src/**/*.xml',
    '/app/src/**/*.json',
  ],
  { query: '?raw', import: 'default', eager: true }
) as Record<string, string>;

const fontModules = import.meta.glob('/app/src/main/res/font/*.ttf', {
  query: '?url',
  import: 'default',
  eager: true,
}) as Record<string, string>;

export interface AndroidProjectFile {
  path: string;
  content: string;
}

export function getAndroidProjectFiles(): AndroidProjectFile[] {
  return Object.entries(rawTextModules)
    .map(([key, content]) => ({
      path: key.replace(/^\/+/, ''),
      content,
    }))
    .sort((a, b) => a.path.localeCompare(b.path));
}

export async function downloadAndroidProjectZip(): Promise<void> {
  const encoder = new TextEncoder();
  const entries = getAndroidProjectFiles().map((f) => ({
    path: `NutriMuscle/${f.path}`,
    data: encoder.encode(f.content),
  }));

  for (const [fontPath, url] of Object.entries(fontModules)) {
    try {
      const resp = await fetch(url);
      if (resp.ok) {
        const buf = await resp.arrayBuffer();
        entries.push({
          path: `NutriMuscle/${fontPath.replace(/^\/+/, '')}`,
          data: new Uint8Array(buf),
        });
      }
    } catch {
      // Skip binary font fetch errors gracefully if unavailable
    }
  }

  const zipBytes = buildZipArchive(entries);
  const blob = new Blob([zipBytes.buffer as ArrayBuffer], {
    type: 'application/zip',
  });
  const url = URL.createObjectURL(blob);
  const a = document.createElement('a');
  a.href = url;
  a.download = 'NutriMuscle-Android.zip';
  document.body.appendChild(a);
  a.click();
  document.body.removeChild(a);
  URL.revokeObjectURL(url);
}
