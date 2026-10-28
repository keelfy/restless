import { defineConfig } from 'vitepress'

export default defineConfig({
  lang: 'ru-RU',
  title: 'Restless',
  description: 'Вики хоррор-мода Restless для Minecraft',
  cleanUrls: true,
  themeConfig: {
    nav: [{ text: 'Духи', link: '/spirits/' }],
    sidebar: [
      {
        text: 'Духи',
        items: [{ text: 'Обзор', link: '/spirits/' }],
      },
    ],
    search: { provider: 'local' },
  },
})
