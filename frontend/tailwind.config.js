/** @type {import('tailwindcss').Config} */
export default {
  content: [
    "./index.html",
    "./src/**/*.{js,ts,jsx,tsx}",
  ],
  theme: {
    extend: {
      colors: {
        canvas: '#F7F6F2',
        card: '#FFFFFF',
        charcoal: '#262B2A',
        muted: '#747A76',
        emerald: {
          DEFAULT: '#17866B',
          hover: '#136F58',
          light: '#E6F5F0',
        },
        violet: {
          DEFAULT: '#8064D8',
          hover: '#6E52C6',
          light: '#F1EEFB',
        },
        coral: {
          DEFAULT: '#E97963',
          hover: '#D6654F',
          light: '#FDEEEC',
        },
        teal: {
          DEFAULT: '#269DA1',
          hover: '#1F8589',
          light: '#E6F6F6',
        },
        border: '#E7E5DF',
      },
    },
  },
  plugins: [],
}
