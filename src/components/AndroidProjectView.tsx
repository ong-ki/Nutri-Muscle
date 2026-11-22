import { useMemo, useState } from 'react';
import { Download, FileCode2, X } from 'lucide-react';
import {
  downloadAndroidProjectZip,
  getAndroidProjectFiles,
} from '../lib/androidProjectLoader';
import { UI_STRINGS } from '../lib/i18n';
import type { AppLanguage } from '../lib/types';

interface AndroidProjectSheetProps {
  language: AppLanguage;
  onClose: () => void;
}

export function AndroidProjectSheet({
  language,
  onClose,
}: AndroidProjectSheetProps) {
  const t = UI_STRINGS[language];
  const files = useMemo(() => getAndroidProjectFiles(), []);
  const [selectedPath, setSelectedPath] = useState<string>(
    files[0]?.path ?? 'app/build.gradle.kts'
  );
  const [filterText, setFilterText] = useState('');

  const filteredFiles = useMemo(() => {
    const q = filterText.trim().toLowerCase();
    if (!q) return files;
    return files.filter((f) => f.path.toLowerCase().includes(q));
  }, [files, filterText]);

  const activeFile =
    files.find((f) => f.path === selectedPath) ?? filteredFiles[0] ?? files[0];

  return (
    <div
      className="fixed inset-0 z-50 flex items-end sm:items-center justify-center bg-slate-950/55 backdrop-blur-[1px] sm:p-4"
      onClick={onClose}
    >
      <div
        role="dialog"
        aria-modal="true"
        aria-label={t.androidProject}
        onClick={(e) => e.stopPropagation()}
        className="w-full max-w-4xl max-h-[88dvh] rounded-t-3xl sm:rounded-2xl bg-white dark:bg-slate-900 border border-slate-200 dark:border-slate-800 flex flex-col overflow-hidden animate-[sheetUp_220ms_ease-out]"
      >
        <div className="px-5 py-4 border-b border-slate-200 dark:border-slate-800 flex items-center justify-between gap-3">
          <div className="min-w-0">
            <h2 className="text-sm font-bold text-slate-900 dark:text-slate-100 truncate">
              {t.androidProject} · {files.length}
            </h2>
            <p className="text-xs text-slate-500 dark:text-slate-400 truncate">
              {t.androidSubtitle}
            </p>
          </div>
          <div className="flex items-center gap-2 shrink-0">
            <button
              type="button"
              onClick={() => void downloadAndroidProjectZip()}
              className="inline-flex items-center gap-1.5 px-3 py-1.5 text-xs font-bold text-white dark:text-slate-950 bg-teal-700 dark:bg-teal-400 rounded-lg cursor-pointer"
            >
              <Download className="w-3.5 h-3.5" />
              <span>{t.downloadZip}</span>
            </button>
            <button
              type="button"
              onClick={onClose}
              aria-label={t.cancel}
              className="p-1.5 rounded-lg text-slate-500 hover:bg-slate-100 dark:hover:bg-slate-800 cursor-pointer"
            >
              <X className="w-4 h-4" />
            </button>
          </div>
        </div>

        <div className="grid grid-cols-1 md:grid-cols-12 flex-1 min-h-0 overflow-hidden">
          <aside className="md:col-span-4 border-b md:border-b-0 md:border-r border-slate-200 dark:border-slate-800 p-3 flex flex-col gap-2 min-h-0 max-h-48 md:max-h-none">
            <input
              type="text"
              value={filterText}
              onChange={(e) => setFilterText(e.target.value)}
              placeholder="Filter…"
              className="w-full px-3 py-1.5 text-xs bg-slate-50 dark:bg-slate-950 border border-slate-200 dark:border-slate-800 rounded-lg text-slate-900 dark:text-slate-100"
            />
            <div className="overflow-y-auto flex-1 space-y-0.5">
              {filteredFiles.map((file) => {
                const active = activeFile?.path === file.path;
                return (
                  <button
                    key={file.path}
                    type="button"
                    onClick={() => setSelectedPath(file.path)}
                    className={`w-full flex items-center gap-2 px-2.5 py-1.5 text-left text-xs rounded-lg cursor-pointer ${
                      active
                        ? 'bg-teal-700/10 dark:bg-teal-400/15 text-teal-800 dark:text-teal-300 font-bold'
                        : 'text-slate-700 dark:text-slate-300 hover:bg-slate-50 dark:hover:bg-slate-800'
                    }`}
                  >
                    <FileCode2 className="w-3.5 h-3.5 shrink-0 opacity-70" />
                    <span className="truncate font-mono">{file.path}</span>
                  </button>
                );
              })}
            </div>
          </aside>

          <section className="md:col-span-8 flex flex-col min-h-0 overflow-hidden">
            <div className="px-4 py-2 border-b border-slate-200 dark:border-slate-800 bg-slate-50 dark:bg-slate-950/60 flex items-center justify-between text-xs font-mono text-slate-600 dark:text-slate-400">
              <span className="truncate">{activeFile?.path}</span>
              <span className="tabular-nums shrink-0 ml-2">
                {activeFile?.content.split('\n').length ?? 0}
              </span>
            </div>
            <pre className="p-4 text-xs font-mono leading-relaxed overflow-auto flex-1 text-slate-800 dark:text-slate-200 bg-white dark:bg-slate-950">
              <code>{activeFile?.content ?? ''}</code>
            </pre>
          </section>
        </div>
      </div>
    </div>
  );
}
