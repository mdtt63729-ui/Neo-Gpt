import React from 'react';
import { Download, RefreshCw, X, Sparkles } from 'lucide-react';
import { motion, AnimatePresence } from 'motion/react';

export type AppUpdate = {
  title?: string;
  versionName?: string;
  versionCode: number;
  description?: string;
  releaseNotes?: string;
  downloadUrl: string;
  fileName?: string;
  publishedAt?: number;
  forceUpdate?: boolean;
};

type Props = {
  update: AppUpdate | null;
  onDismiss: () => void;
};

export function UpdateGate({ update, onDismiss }: Props) {
  const openDownload = () => {
    if (!update?.downloadUrl) return;
    window.open(update.downloadUrl, '_blank', 'noopener,noreferrer');
  };

  return (
    <AnimatePresence>
      {update && (
        <motion.div
          className="neo-update-overlay"
          initial={{ opacity: 0 }}
          animate={{ opacity: 1 }}
          exit={{ opacity: 0 }}
        >
          <motion.div
            className="neo-update-card"
            initial={{ opacity: 0, y: 28, scale: .97 }}
            animate={{ opacity: 1, y: 0, scale: 1 }}
            transition={{ duration: .42, ease: [0.22, 1, 0.36, 1] }}
          >
            <div className="neo-update-icon"><Sparkles size={24} /></div>
            <div className="neo-update-kicker">{update.forceUpdate ? 'REQUIRED UPDATE' : 'NEW UPDATE AVAILABLE'}</div>
            <h1>{update.title || `Neo Gpt ${update.versionName || ''}`}</h1>
            <div className="neo-update-version">Version {update.versionName || 'New'} · Build {update.versionCode}</div>
            {update.description && <p className="neo-update-description">{update.description}</p>}
            {update.releaseNotes && (
              <div className="neo-update-notes">
                <div className="neo-update-notes-title">What’s new</div>
                <div>{update.releaseNotes}</div>
              </div>
            )}
            <button type="button" className="neo-update-download" onClick={openDownload}>
              <Download size={19} />
              {update.forceUpdate ? 'Update now' : 'Download update'}
            </button>
            {!update.forceUpdate && (
              <button type="button" className="neo-update-later" onClick={onDismiss}>
                <X size={16} /> Later
              </button>
            )}
            {update.forceUpdate && <div className="neo-update-required"><RefreshCw size={13} /> Update required to continue</div>}
          </motion.div>
        </motion.div>
      )}
    </AnimatePresence>
  );
}
