const Favorites = () => import(/* webpackChunkName: "js/favorite/favorite" */'@/views/favorite/Favorites')

const favoriteRoutes = [
  {
    path: '/favorites',
    component: Favorites
  }
]
export default favoriteRoutes
