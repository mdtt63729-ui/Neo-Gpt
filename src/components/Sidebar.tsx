import React from 'react';
import { motion, AnimatePresence } from 'motion/react';
import { Search, Image as ImageIcon, Library, Folder, Clock, Puzzle, Edit2, Settings as SettingsIcon, Trash2 } from 'lucide-react';
import { haptic } from '../lib/haptics';

interface SidebarProps {
  isOpen: boolean;
  onClose: () => void;
  onOpenSettings: () => void;
  onNewChat: () => void;
  chatHistory: { id: string; title: string }[];
  onSelectChat: (id: string) => void;
  onDeleteChat: (id: string) => void;
  isAuthenticated: boolean;
  onLogin: () => void;
}

export function Sidebar({ isOpen, onClose, onOpenSettings, onNewChat, chatHistory, onSelectChat, onDeleteChat, isAuthenticated, onLogin }: SidebarProps) {
  const notify = (message: string) => {
    window.dispatchEvent(new CustomEvent('neo-gpt-toast', { detail: message }));
  };

  return (
    <AnimatePresence>
      {isOpen && (
        <>
          <motion.div
            initial={{ opacity: 0 }}
            animate={{ opacity: 1 }}
            exit={{ opacity: 0 }}
            transition={{ duration: 0.22, ease: [0.22, 1, 0.36, 1] }}
            onPointerDown={onClose}
            aria-hidden="true"
            className="absolute inset-0 bg-black/20 z-[110] backdrop-blur-[2px]"
          />
          <motion.div
            initial={{ x: '-104%' }}
            animate={{ x: 0 }}
            exit={{ x: '-104%' }}
            transition={{ type: 'spring', stiffness: 430, damping: 38, mass: 0.72 }}
            className="absolute inset-y-0 left-0 w-[min(82%,320px)] bg-white dark:bg-[#121212] z-[120] flex flex-col shadow-xl will-change-transform"
            onPointerDown={(event) => event.stopPropagation()}
          >
            <div className="flex items-center justify-between p-4 px-6 neo-sidebar-header">
              <h2 className="text-2xl font-semibold text-gray-900 dark:text-white tracking-tight">Neo Gpt</h2>
              <div className="flex gap-4">
                <motion.button type="button" whileTap={{ scale: 0.94 }} onClick={() => { haptic('tap'); notify('Search is ready for a future update.'); }} className="p-2 bg-gray-100 dark:bg-zinc-800 rounded-full hover:bg-gray-200 dark:hover:bg-zinc-700 transition-colors">
                  <Search size={20} className="text-gray-700 dark:text-gray-200" />
                </motion.button>
              </div>
            </div>

            <div className="flex-1 overflow-y-auto px-4 py-2">
              <nav className="space-y-1 mb-8">
                <div className="px-2 py-3 text-xs font-semibold uppercase tracking-wide text-gray-400 dark:text-gray-500">Conversations</div>
              </nav>

              <div className="border-t border-gray-100 dark:border-zinc-800 pt-6">
                <div className="px-2">
                  <div className="px-2 mb-3 text-xs font-semibold uppercase tracking-wide text-gray-400 dark:text-gray-500">Chat history</div>
                  {chatHistory.length === 0 ? (
                    <p className="px-2 py-4 text-sm text-gray-400 dark:text-gray-500">No chat history yet</p>
                  ) : (
                    <div className="space-y-1">
                      {chatHistory.map(chat => (
                        <div key={chat.id} className="flex items-center gap-1 rounded-2xl hover:bg-gray-50 dark:hover:bg-zinc-800 transition-colors">
                          <motion.button
                            type="button"
                            whileTap={{ scale: 0.97 }}
                            onClick={() => { haptic('selection'); onSelectChat(chat.id); }}
                            className="flex-1 min-w-0 text-left px-3 py-3 rounded-2xl"
                          >
                            <div className="text-sm font-medium text-gray-700 dark:text-gray-200 truncate">{chat.title}</div>
                          </motion.button>
                          <motion.button
                            type="button"
                            whileTap={{ scale: 0.86 }}
                            aria-label={`Delete ${chat.title}`}
                            onClick={(event) => { event.stopPropagation(); haptic('warning'); if (window.confirm(`Delete this chat?`)) onDeleteChat(chat.id); }}
                            className="p-2.5 mr-1 rounded-full text-gray-400 hover:text-red-500 hover:bg-red-50 dark:hover:bg-red-900/20 flex-shrink-0"
                          >
                            <Trash2 size={17} />
                          </motion.button>
                        </div>
                      ))}
                    </div>
                  )}
                </div>
              </div>
            </div>

            <div className="p-4 border-t border-gray-100 dark:border-zinc-800 bg-white dark:bg-[#121212] neo-sidebar-footer">
              {isAuthenticated ? (
                <div className="flex items-center justify-between">
                  <motion.button
                    type="button"
                    whileTap={{ scale: 0.94 }}
                    onClick={() => { haptic('tap'); onNewChat(); }}
                    className="flex-1 flex items-center justify-center gap-2 bg-blue-500 text-white px-5 py-3.5 rounded-full font-medium hover:bg-blue-600 transition-colors shadow-sm mr-3"
                  >
                    <Edit2 size={18} />
                    New Chat
                  </motion.button>

                  <motion.button
                    type="button"
                    whileTap={{ scale: 0.92 }}
                    onClick={() => {
                      haptic('tap');
                      onClose();
                      onOpenSettings();
                    }}
                    className="w-12 h-12 bg-gray-100 dark:bg-zinc-800 rounded-full flex items-center justify-center text-gray-700 dark:text-gray-300 hover:bg-gray-200 dark:hover:bg-zinc-700 transition-colors flex-shrink-0"
                  >
                    <SettingsIcon size={22} />
                  </motion.button>
                </div>
              ) : (
                <motion.button
                  type="button"
                  whileTap={{ scale: 0.96 }}
                  onClick={() => { haptic('tap'); onClose(); onLogin(); }}
                  className="w-full flex items-center justify-center gap-2 bg-blue-500 text-white px-5 py-3.5 rounded-full font-medium hover:bg-blue-600 transition-colors shadow-sm"
                >
                  Login
                </motion.button>
              )}
            </div>
          </motion.div>
        </>
      )}
    </AnimatePresence>
  );
}
