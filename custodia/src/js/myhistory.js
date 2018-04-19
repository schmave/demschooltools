import { useRouterHistory } from 'react-router'
import { createHashHistory } from 'history'

module.exports = useRouterHistory(createHashHistory)({ queryKey: false });
