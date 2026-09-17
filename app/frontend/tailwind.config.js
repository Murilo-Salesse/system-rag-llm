/** @type {import('tailwindcss').Config} */
export default {
  content: [
    "./index.html",
    "./src/**/*.{js,ts,jsx,tsx}",
  ],
  theme: {
    extend: {
      colors: {
        primary: '#8e8ea0',
      },
      borderRadius: {
        design: '5px',
      },
      transitionDuration: {
        design: '400ms',
      },
      transitionTimingFunction: {
        design: 'ease',
      },
    },
  },
  plugins: [],
}
