import React from 'react';
import { motion, AnimatePresence } from 'motion/react';
import { Search, RefreshCw, Image as ImageIcon, Library, Folder, Clock, Puzzle, MessageSquare, Edit2, Settings as SettingsIcon } from 'lucide-react';
import { cn } from '../lib/utils';

interface SidebarProps {
  isOpen: boolean;
  onClose: () => void;
  onOpenSettings: () => void;
  onNewChat: () => void;
  chatHistory: { id: string; title: string }[];
  onSelectChat: (id: string) => void;
}

export function Sidebar({ isOpen, onClose, onOpenSettings, onNewChat, chatHistory, onSelectChat }: SidebarProps) {
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
            onClick={onClose}
            className="absolute inset-0 bg-black/20 z-40 backdrop-blur-sm"
          />
          <motion.div
            initial={{ x: '-100%' }}
            animate={{ x: 0 }}
            exit={{ x: '-100%' }}
            transition={{ type: 'spring', bounce: 0, duration: 0.3 }}
            className="absolute inset-y-0 left-0 w-[80%] max-w-[320px] bg-white dark:bg-[#121212] z-50 flex flex-col shadow-xl"
          >
            <div className="flex items-center justify-between p-4 px-6 neo-sidebar-header">
              <h2 className="text-2xl font-semibold text-gray-900 dark:text-white tracking-tight">Neo Gpt</h2>
              <div className="flex gap-4">
                <motion.button type="button" whileTap={{ scale: 0.94 }} onClick={() => notify('Search is ready for a future update.')} className="p-2 bg-gray-100 dark:bg-zinc-800 rounded-full hover:bg-gray-200 dark:hover:bg-zinc-700 transition-colors">
                  <Search size={20} className="text-gray-700 dark:text-gray-200" />
                </motion.button>
              </div>
            </div>

            <div className="flex-1 overflow-y-auto px-4 py-2">
              <nav className="space-y-1 mb-8">
                <motion.button type="button" whileTap={{ scale: 0.96 }} onClick={() => notify('This section is not configured yet.')} className="w-full flex items-center gap-4 px-4 py-3 hover:bg-gray-50 dark:hover:bg-zinc-800 rounded-[20px] text-gray-700 dark:text-gray-200 font-medium transition-colors">
                  <ImageIcon size={22} className="text-gray-900 dark:text-white" />
                  Images
                </motion.button>
                <motion.button type="button" whileTap={{ scale: 0.96 }} onClick={() => notify('This section is not configured yet.')} className="w-full flex items-center gap-4 px-4 py-3 hover:bg-gray-50 dark:hover:bg-zinc-800 rounded-[20px] text-gray-700 dark:text-gray-200 font-medium transition-colors">
                  <Library size={22} className="text-gray-900 dark:text-white" />
                  Library
                </motion.button>
                <motion.button type="button" whileTap={{ scale: 0.96 }} onClick={() => notify('This section is not configured yet.')} className="w-full flex items-center gap-4 px-4 py-3 hover:bg-gray-50 dark:hover:bg-zinc-800 rounded-[20px] text-gray-700 dark:text-gray-200 font-medium transition-colors">
                  <Folder size={22} className="text-gray-900 dark:text-white" />
                  Projects
                </motion.button>
                <motion.button type="button" whileTap={{ scale: 0.96 }} onClick={() => notify('This section is not configured yet.')} className="w-full flex items-center gap-4 px-4 py-3 hover:bg-gray-50 dark:hover:bg-zinc-800 rounded-[20px] text-gray-700 dark:text-gray-200 font-medium transition-colors">
                  <Clock size={22} className="text-gray-900 dark:text-white" />
                  Scheduled
                </motion.button>
                <motion.button type="button" whileTap={{ scale: 0.96 }} onClick={() => notify('This section is not configured yet.')} className="w-full flex items-center gap-4 px-4 py-3 hover:bg-gray-50 dark:hover:bg-zinc-800 rounded-[20px] text-gray-700 dark:text-gray-200 font-medium transition-colors">
                  <Puzzle size={22} className="text-gray-900 dark:text-white" />
                  Plugins
                </motion.button>
              </nav>

              <div className="border-t border-gray-100 dark:border-zinc-800 pt-6">
                <div className="px-2">
                  <div className="px-2 mb-3 text-xs font-semibold uppercase tracking-wide text-gray-400 dark:text-gray-500">Chat history</div>
                  {chatHistory.length === 0 ? (
                    <p className="px-2 py-4 text-sm text-gray-400 dark:text-gray-500">No chat history yet</p>
                  ) : (
                    <div className="space-y-1">
                      {chatHistory.map(chat => (
                        <motion.button
                          type="button"
                          key={chat.id}
                          whileTap={{ scale: 0.97 }}
                          onClick={() => onSelectChat(chat.id)}
                          className="w-full text-left px-3 py-3 rounded-2xl hover:bg-gray-50 dark:hover:bg-zinc-800 transition-colors"
                        >
                          <div className="text-sm font-medium text-gray-700 dark:text-gray-200 truncate">{chat.title}</div>
                        </motion.button>
                      ))}
                    </div>
                  )}
                </div>
              </div>
            </div>

            <div className="p-4 border-t border-gray-100 dark:border-zinc-800 flex items-center justify-between bg-white dark:bg-[#121212] neo-sidebar-footer">
              <motion.button 
                type="button"
                whileTap={{ scale: 0.94 }}
                onClick={onNewChat}
                className="flex-1 flex items-center justify-center gap-2 bg-blue-500 text-white px-5 py-3.5 rounded-full font-medium hover:bg-blue-600 transition-colors shadow-sm mr-3"
              >
                <Edit2 size={18} />
                New Chat
              </motion.button>
              
              <motion.button 
                type="button"
                whileTap={{ scale: 0.92 }}
                onClick={() => {
                  onClose();
                  onOpenSettings();
                }} 
                className="w-12 h-12 bg-gray-100 dark:bg-zinc-800 rounded-full flex items-center justify-center text-gray-700 dark:text-gray-300 hover:bg-gray-200 dark:hover:bg-zinc-700 transition-colors flex-shrink-0"
              >
                <SettingsIcon size={22} />
              </motion.button>
            </div>
          </motion.div>
        </>
      )}
    </AnimatePresence>
  );
}
